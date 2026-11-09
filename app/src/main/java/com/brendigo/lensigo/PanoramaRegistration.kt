package com.brendigo.lensigo

import android.graphics.Bitmap
import android.graphics.Matrix
import java.util.Random
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Offline, bounded perspective registration.
 * Matches local gradient patches and estimates a projective 3x3 transform
 * with deterministic RANSAC. Returns null when the overlap is ambiguous;
 * callers fall back to the guided fixed-overlap montage.
 *
 * No native library, network access, or imported photo data required.
 */
internal object PanoramaRegistration {
    private data class Feature(val x: Int, val y: Int, val score: Int)
    private data class Match(val x: Float, val y: Float, val dx: Float, val dy: Float)

    fun estimate(previous: Bitmap, next: Bitmap, nominalAdvance: Int): Matrix? {
        if (previous.width != next.width || previous.height != next.height) return null
        val w = next.width
        val h = next.height
        if (w < 160 || h < 120) return null
        val prev = grayscale(previous)
        val curr = grayscale(next)
        val prevCorners = features(prev,w,h,(w*.62f).toInt(),(w*.96f).toInt())
        val nextCorners = features(curr,w,h,(w*.04f).toInt(),(w*.37f).toInt())
        if (prevCorners.size < 10 || nextCorners.size < 10) return null
        val matches = ArrayList<Match>()
        for (source in nextCorners) {
            var best = Int.MAX_VALUE
            var second = Int.MAX_VALUE
            var match: Feature? = null
            for (candidate in prevCorners) {
                if (abs(candidate.x - source.x - nominalAdvance) > w*.17f ||
                    abs(candidate.y - source.y) > h*.14f) continue
                val distance = descriptorDistance(curr, prev, w, source, candidate)
                if (distance < best) {
                    second = best
                    best = distance
                    match = candidate
                } else if (distance < second) second = distance
            }
            if (match != null && best < 730 && best.toFloat() < second*.79f) {
                matches += Match(
                    source.x.toFloat(), source.y.toFloat(),
                    match.x.toFloat(), match.y.toFloat()
                )
            }
        }
        if (matches.size < 9) return null

        val random = Random(9_768_433L)
        var bestMatrix: Matrix? = null
        var bestCount = 0
        var bestError = Float.POSITIVE_INFINITY
        repeat(140) {
            val indices = HashSet<Int>()
            while (indices.size < 4) indices += random.nextInt(matches.size)
            val sample = indices.map { matches[it] }
            val src = FloatArray(8)
            val dst = FloatArray(8)
            sample.forEachIndexed { k,m ->
                src[2*k] = m.x; src[2*k+1] = m.y
                dst[2*k] = m.dx; dst[2*k+1] = m.dy
            }
            val model = Matrix()
            if (!model.setPolyToPoly(src,0,dst,0,4) ||
                !plausible(model,w,h,nominalAdvance)) return@repeat
            val projected = FloatArray(matches.size*2)
            for (i in matches.indices) {
                projected[2*i] = matches[i].x
                projected[2*i+1] = matches[i].y
            }
            model.mapPoints(projected)
            var inliers = 0
            var error = 0f
            matches.forEachIndexed { i,m ->
                val distance = hypot(projected[2*i]-m.dx,
                    projected[2*i+1]-m.dy)
                if (distance < 7f) {
                    inliers++
                    error += distance
                }
            }
            if (inliers > bestCount || (inliers == bestCount && error < bestError)) {
                bestCount = inliers
                bestError = error
                bestMatrix = Matrix(model)
            }
        }
        // A minimum consensus prevents destructive warps on empty skies,
        // moving waves, and frames with insufficient common scene details.
        return if (bestCount >= max(7,matches.size/3) &&
            bestError/bestCount < 4.5f) bestMatrix else null
    }

    private fun plausible(matrix: Matrix,w:Int,h:Int,shift:Int):Boolean {
        val corners=floatArrayOf(0f,0f,w.toFloat(),0f,0f,h.toFloat(),
            w.toFloat(),h.toFloat())
        matrix.mapPoints(corners)
        if(corners.any{!it.isFinite()})return false
        val left=min(corners[0],corners[4])
        val right=max(corners[2],corners[6])
        val top=min(corners[1],corners[3])
        val bottom=max(corners[5],corners[7])
        return left in (shift-w*.20f)..(shift+w*.20f) &&
            right-left in (w*.65f)..(w*1.35f) &&
            top in (-h*.18f)..(h*.18f) &&
            bottom-top in (h*.77f)..(h*1.23f)
    }

    private fun grayscale(bitmap:Bitmap):IntArray {
        val pixels=IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        for(i in pixels.indices){
            val c=pixels[i]
            pixels[i]=((c ushr 16 and 255)*77+(c ushr 8 and 255)*150+
                (c and 255)*29) ushr 8
        }
        return pixels
    }

    private fun features(gray:IntArray,w:Int,h:Int,left:Int,right:Int):List<Feature> {
        val items=ArrayList<Feature>()
        for(cy in (h*.09f).toInt() until (h*.91f).toInt() step 18) {
            for(cx in max(8,left) until min(w-8,right) step 18) {
                var bestScore=0
                var bx=0
                var by=0
                for(y in cy until min(cy+18,h-8) step 3) {
                    for(x in cx until min(cx+18,w-8) step 3) {
                        val gx=gray[y*w+x+4]-gray[y*w+x-4]
                        val gy=gray[(y+4)*w+x]-gray[(y-4)*w+x]
                        val score=abs(gx*gy)
                        if(score>bestScore){bestScore=score;bx=x;by=y}
                    }
                }
                if(bestScore>340)items+=Feature(bx,by,bestScore)
            }
        }
        return items.sortedByDescending{it.score}.take(145)
    }

    private fun descriptorDistance(a:IntArray,b:IntArray,w:Int,
        source:Feature,target:Feature):Int{
        val baseA=a[source.y*w+source.x]
        val baseB=b[target.y*w+target.x]
        var distance=0
        for(y in -6..6 step 3)for(x in -6..6 step 3){
            distance+=abs((a[(source.y+y)*w+source.x+x]-baseA)-
                (b[(target.y+y)*w+target.x+x]-baseB))
        }
        return distance
    }
}
