package com.example.service

import android.graphics.Path
import android.graphics.PointF
import com.example.model.HumanizeConfig
import java.util.Random
import kotlin.math.max
import kotlin.math.min

object Humanizer {

    private val random = Random(System.currentTimeMillis())

    /**
     * Applies Gaussian spatial jitter around target coordinate to mimic natural finger touch.
     * Uses [HumanBehaviorUtility] for biomechanical touch modeling.
     */
    fun applyJitter(
        targetX: Float,
        targetY: Float,
        jitterRadiusPx: Float,
        config: HumanizeConfig
    ): PointF {
        if (!config.enabled || jitterRadiusPx <= 0f) {
            return PointF(targetX, targetY)
        }
        val result = HumanBehaviorUtility.calculateHumanizedPoint(
            targetX = targetX,
            targetY = targetY,
            radiusPx = jitterRadiusPx,
            profile = HumanBehaviorUtility.MotorProfile.NATURAL
        )
        return PointF(result.x, result.y)
    }

    /**
     * Calculates randomized delay to break periodic robotic intervals.
     * Incorporates skewed log-normal distribution & cognitive micro-hesitations from [HumanBehaviorUtility].
     */
    fun computeRandomizedDelay(
        baseDelayMs: Long,
        varianceMs: Long,
        config: HumanizeConfig,
        speedScale: Float = 1.0f
    ): Long {
        if (!config.enabled) {
            return max(10L, (baseDelayMs * speedScale).toLong())
        }

        val effectiveVariance = if (varianceMs > 0) {
            ((varianceMs.toFloat() / max(1L, baseDelayMs)) * 100f).coerceIn(5f, 60f)
        } else {
            config.timeVariancePercentage
        }

        val timingResult = HumanBehaviorUtility.computeVaryingDelay(
            baseDelayMs = baseDelayMs,
            variancePercentage = effectiveVariance,
            profile = HumanBehaviorUtility.MotorProfile.NATURAL,
            allowHesitation = config.microPauseProbability > 0.01f,
            speedScale = speedScale
        )

        return timingResult.totalDelayMs
    }

    /**
     * Computes natural human press duration (average ~70-110ms with slight variance).
     */
    fun computePressDuration(baseDurationMs: Long, config: HumanizeConfig): Long {
        if (!config.enabled) return max(30L, baseDurationMs)
        return HumanBehaviorUtility.computePressDuration(baseDurationMs = baseDurationMs, varianceMs = 18L)
    }

    /**
     * Generates a curved Path using quadratic Bezier curves for human swipe emulation.
     */
    fun createNaturalSwipePath(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        config: HumanizeConfig
    ): Path {
        if (!config.enabled || !config.naturalBezierCurves) {
            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
            return path
        }

        return HumanBehaviorUtility.buildCurvedSwipePath(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            jitterRadiusPx = 4.0f,
            profile = HumanBehaviorUtility.MotorProfile.NATURAL
        )
    }

    /**
     * Calculates anti-bot defense score (0..100) based on jitter, timing variance, and curve parameters.
     */
    fun calculateAntiBotScore(config: HumanizeConfig): Int {
        if (!config.enabled) return 15
        var score = 50
        if (config.jitterRadiusPx in 3f..12f) score += 20
        if (config.timeVariancePercentage >= 10f) score += 15
        if (config.naturalBezierCurves) score += 10
        if (config.microPauseProbability > 0.02f) score += 5
        return min(100, score)
    }
}

