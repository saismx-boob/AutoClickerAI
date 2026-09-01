package com.example.service

import android.graphics.Path
import android.graphics.PointF
import com.example.model.HumanizeConfig
import java.util.Random
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility class that introduces realistic random micro-offsets to click coordinates
 * and varying delays between taps to mimic genuine human touch behavior and evade
 * robotic pattern detection algorithms.
 */
object HumanBehaviorUtility {

    private val random = Random(System.currentTimeMillis())

    /**
     * Preset profiles adjusting motor precision, timing variability, and cognitive hesitation.
     */
    enum class MotorProfile(
        val displayName: String,
        val jitterScale: Float,
        val timingVarianceRatio: Float,
        val hesitationProbability: Float,
        val description: String
    ) {
        STEALTH(
            displayName = "Anti-Détection Furtif",
            jitterScale = 1.35f,
            timingVarianceRatio = 0.28f,
            hesitationProbability = 0.10f,
            description = "Variations spatiales et temporelles maximales pour contourner les protections anti-bot."
        ),
        NATURAL(
            displayName = "Humain Naturel",
            jitterScale = 1.0f,
            timingVarianceRatio = 0.18f,
            hesitationProbability = 0.05f,
            description = "Comportement biomécanique standard équilibré avec dispersion gaussienne."
        ),
        GAMER_FAST(
            displayName = "Réflexe Rapide",
            jitterScale = 0.65f,
            timingVarianceRatio = 0.10f,
            hesitationProbability = 0.02f,
            description = "Temps de réaction rapides et clics précis avec micro-variations subtiles."
        ),
        CASUAL_RELAXED(
            displayName = "Navigation Détente",
            jitterScale = 1.2f,
            timingVarianceRatio = 0.35f,
            hesitationProbability = 0.14f,
            description = "Rythme plus lent avec pauses occasionnelles de lecture/observation."
        )
    }

    /**
     * Represents the calculated humanized coordinate result.
     */
    data class HumanizedCoordinate(
        val x: Float,
        val y: Float,
        val originalX: Float,
        val originalY: Float,
        val deltaX: Float,
        val deltaY: Float,
        val releaseX: Float,
        val releaseY: Float
    )

    /**
     * Represents the calculated humanized timing result.
     */
    data class HumanizedTiming(
        val totalDelayMs: Long,
        val baseDelayMs: Long,
        val deltaDelayMs: Long,
        val pressDurationMs: Long,
        val isHesitationApplied: Boolean
    )

    // =========================================================================
    // SPATIAL HUMANIZATION: Micro-Offsets & Contact Geometry
    // =========================================================================

    /**
     * Generates a bivariate Gaussian (Box-Muller) micro-offset around target coordinates.
     * Real finger taps conform to a normal distribution decaying rapidly from center.
     *
     * @param targetX Original target X coordinate (pixels)
     * @param targetY Original target Y coordinate (pixels)
     * @param radiusPx Maximum expected standard deviation radius in pixels (default: 6px)
     * @param profile Desired motor profile
     * @param screenWidth Width of the display for boundary clamping (default: 1080px)
     * @param screenHeight Height of the display for boundary clamping (default: 2400px)
     * @return [HumanizedCoordinate] containing adjusted touch-down and touch-release coordinates.
     */
    fun calculateHumanizedPoint(
        targetX: Float,
        targetY: Float,
        radiusPx: Float = 6.0f,
        profile: MotorProfile = MotorProfile.NATURAL,
        screenWidth: Float = 1080f,
        screenHeight: Float = 2400f
    ): HumanizedCoordinate {
        if (radiusPx <= 0.1f) {
            return HumanizedCoordinate(
                x = targetX,
                y = targetY,
                originalX = targetX,
                originalY = targetY,
                deltaX = 0f,
                deltaY = 0f,
                releaseX = targetX,
                releaseY = targetY
            )
        }

        val effectiveRadius = radiusPx * profile.jitterScale
        val sigma = effectiveRadius / 2.5f

        // Box-Muller Gaussian transform for radial distribution
        val u1 = max(0.00001f, random.nextFloat())
        val u2 = random.nextFloat()
        val magnitude = sqrt(-2.0 * ln(u1.toDouble())) * sigma
        val angle = 2.0 * Math.PI * u2

        // Biomechanical aspect: Thumb contact pad is slightly elongated along a ~35° diagonal
        val elongationFactor = 1.18f
        val tiltAngleRad = Math.toRadians(35.0)

        val unrotatedDx = (magnitude * cos(angle)).toFloat()
        val unrotatedDy = ((magnitude * sin(angle)) * elongationFactor).toFloat()

        // Apply biomechanical rotation
        val dx = (unrotatedDx * cos(tiltAngleRad) - unrotatedDy * sin(tiltAngleRad)).toFloat()
        val dy = (unrotatedDx * sin(tiltAngleRad) + unrotatedDy * cos(tiltAngleRad)).toFloat()

        val clampedX = (targetX + dx).coerceIn(4f, max(4f, screenWidth - 4f))
        val clampedY = (targetY + dy).coerceIn(4f, max(4f, screenHeight - 4f))

        // Natural micro-slip vector between finger contact down and lift up (~0.5 - 1.8 px)
        val slipAngle = angle + (random.nextFloat() * 0.4f - 0.2f)
        val slipMagnitude = (0.4f + random.nextFloat() * 1.2f)
        val releaseX = (clampedX + cos(slipAngle).toFloat() * slipMagnitude).coerceIn(0f, screenWidth)
        val releaseY = (clampedY + sin(slipAngle).toFloat() * slipMagnitude).coerceIn(0f, screenHeight)

        return HumanizedCoordinate(
            x = clampedX,
            y = clampedY,
            originalX = targetX,
            originalY = targetY,
            deltaX = clampedX - targetX,
            deltaY = clampedY - targetY,
            releaseX = releaseX,
            releaseY = releaseY
        )
    }

    /**
     * Convenience method to directly obtain a [PointF] with random Gaussian micro-offset.
     */
    fun getOffsetCoordinate(
        targetX: Float,
        targetY: Float,
        maxRadiusPx: Float = 6.0f,
        config: HumanizeConfig? = null
    ): PointF {
        if (config != null && !config.enabled) {
            return PointF(targetX, targetY)
        }
        val radius = config?.jitterRadiusPx ?: maxRadiusPx
        val result = calculateHumanizedPoint(targetX, targetY, radius)
        return PointF(result.x, result.y)
    }

    /**
     * Generates a slow postural drift offset that subtly shifts coordinate baselines
     * across consecutive executions (simulating minor phone readjustment in hands).
     */
    fun computePosturalDrift(iterationIndex: Int, maxDriftPx: Float = 2.5f): PointF {
        val frequency = 0.08
        val driftX = (sin(iterationIndex * frequency) * maxDriftPx).toFloat()
        val driftY = (cos(iterationIndex * frequency * 0.7) * (maxDriftPx * 0.8f)).toFloat()
        return PointF(driftX, driftY)
    }

    // =========================================================================
    // TEMPORAL HUMANIZATION: Varying Delays, Skewed Distributions & Pauses
    // =========================================================================

    /**
     * Computes a highly realistic varying delay following a right-skewed Log-Normal distribution.
     * Human reaction times and inter-tap intervals are mathematically non-symmetric:
     * they have a hard lower physical bound (~60-100ms) with a long tail of occasional slower actions.
     *
     * @param baseDelayMs Baseline interval in milliseconds
     * @param variancePercentage Percentage of variance to inject (0..100)
     * @param profile Motor/timing profile
     * @param allowHesitation Whether to randomly insert human visual cognitive pauses
     * @return [HumanizedTiming] with the calculated delay and press duration
     */
    fun computeVaryingDelay(
        baseDelayMs: Long,
        variancePercentage: Float = 15f,
        profile: MotorProfile = MotorProfile.NATURAL,
        allowHesitation: Boolean = true,
        speedScale: Float = 1.0f
    ): HumanizedTiming {
        val scaledBase = max(10L, (baseDelayMs * speedScale).toLong())
        val effectiveVariance = max(0.05f, (variancePercentage / 100f) * profile.timingVarianceRatio)

        // Log-normal stochastic generator: ln(X) ~ N(mu, sigma^2)
        val sigma = (effectiveVariance * 1.1).coerceIn(0.04, 0.6)
        val mu = ln(scaledBase.toDouble()) - (sigma * sigma / 2.0)
        val gaussianSample = random.nextGaussian()
        val logNormalDelay = exp(mu + sigma * gaussianSample).toLong()

        // Bounded variance limit (protect against extreme mathematical tail values)
        val minDelay = max(15L, (scaledBase * 0.45).toLong())
        val maxDelay = (scaledBase * 2.8).toLong()
        var actualDelay = logNormalDelay.coerceIn(minDelay, maxDelay)

        // Check for cognitive hesitation (e.g. human user verifying screen content)
        var hadHesitation = false
        if (allowHesitation && random.nextFloat() < profile.hesitationProbability) {
            val hesitationDuration = (random.nextInt(250) + 120).toLong()
            actualDelay += hesitationDuration
            hadHesitation = true
        }

        // Realistic touch-down contact duration (~55ms to 120ms)
        val pressBase = 80L
        val pressJitter = (random.nextGaussian() * 12.0).toLong()
        val pressDuration = (pressBase + pressJitter).coerceIn(35L, 220L)

        return HumanizedTiming(
            totalDelayMs = actualDelay,
            baseDelayMs = scaledBase,
            deltaDelayMs = actualDelay - scaledBase,
            pressDurationMs = pressDuration,
            isHesitationApplied = hadHesitation
        )
    }

    /**
     * Computes a natural human press duration for tap gestures.
     */
    fun computePressDuration(baseDurationMs: Long = 85L, varianceMs: Long = 15L): Long {
        val jitter = (random.nextGaussian() * (varianceMs / 2.0)).toLong()
        return (baseDurationMs + jitter).coerceIn(35L, 300L)
    }

    /**
     * Generates a sequence of naturally varying inter-tap intervals for rapid multi-tap actions.
     * Enforces human neuromuscular refractory minimums (~50ms - 90ms).
     */
    fun generateMultiTapSequence(
        tapCount: Int,
        averageIntervalMs: Long = 110L
    ): List<Long> {
        val intervals = mutableListOf<Long>()
        for (i in 0 until tapCount) {
            val jitter = (random.nextGaussian() * 18.0).toLong()
            val interval = (averageIntervalMs + jitter).coerceIn(55L, 280L)
            intervals.add(interval)
        }
        return intervals
    }

    // =========================================================================
    // GESTURE TRAJECTORY: Natural Bezier Curve Generation
    // =========================================================================

    /**
     * Builds an authentic curved swipe trajectory with dynamic velocity profile.
     * Human fingers follow curved arcs rather than perfectly straight geometric vectors.
     */
    fun buildCurvedSwipePath(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        jitterRadiusPx: Float = 4.0f,
        profile: MotorProfile = MotorProfile.NATURAL
    ): Path {
        val path = Path()
        val startPoint = calculateHumanizedPoint(startX, startY, jitterRadiusPx, profile)
        val endPoint = calculateHumanizedPoint(endX, endY, jitterRadiusPx, profile)

        path.moveTo(startPoint.x, startPoint.y)

        val midX = (startPoint.x + endPoint.x) / 2f
        val midY = (startPoint.y + endPoint.y) / 2f
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y

        // Perpendicular arc curvature (simulates natural forearm/wrist pivot)
        val curvature = (random.nextFloat() * 0.18f - 0.09f) * profile.jitterScale
        val controlX = midX - dy * curvature
        val controlY = midY + dx * curvature

        path.quadTo(controlX, controlY, endPoint.x, endPoint.y)
        return path
    }
}
