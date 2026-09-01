package com.example

import com.example.service.HumanBehaviorUtility
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class HumanBehaviorUtilityTest {

    @Test
    fun calculateHumanizedPoint_withZeroRadius_returnsExactCoordinates() {
        val result = HumanBehaviorUtility.calculateHumanizedPoint(
            targetX = 500f,
            targetY = 1000f,
            radiusPx = 0f
        )
        assertEquals(500f, result.x, 0.001f)
        assertEquals(1000f, result.y, 0.001f)
        assertEquals(0f, result.deltaX, 0.001f)
        assertEquals(0f, result.deltaY, 0.001f)
    }

    @Test
    fun calculateHumanizedPoint_withRadius_introducesRealisticOffset() {
        val targetX = 540f
        val targetY = 960f
        val radius = 10f

        val points = (1..50).map {
            HumanBehaviorUtility.calculateHumanizedPoint(
                targetX = targetX,
                targetY = targetY,
                radiusPx = radius,
                profile = HumanBehaviorUtility.MotorProfile.NATURAL
            )
        }

        // Verify points vary and are not identical
        val uniquePoints = points.map { Pair(it.x, it.y) }.toSet()
        assertTrue("Points should have stochastic variation", uniquePoints.size > 30)

        // Verify all points remain close to target within reasonable Gaussian bounds
        points.forEach { point ->
            val dist = hypot((point.x - targetX).toDouble(), (point.y - targetY).toDouble())
            assertTrue("Offset dist should be reasonable: $dist", dist < radius * 4.0)
            assertTrue("Release coordinate should exist", point.releaseX > 0f)
        }
    }

    @Test
    fun calculateHumanizedPoint_respectsScreenBoundaries() {
        val screenW = 1080f
        val screenH = 2400f

        // Corner near (0, 0)
        val cornerTopLeft = HumanBehaviorUtility.calculateHumanizedPoint(
            targetX = 2f,
            targetY = 2f,
            radiusPx = 25f,
            screenWidth = screenW,
            screenHeight = screenH
        )
        assertTrue("X must be inside screen", cornerTopLeft.x in 4f..screenW)
        assertTrue("Y must be inside screen", cornerTopLeft.y in 4f..screenH)

        // Corner near (1080, 2400)
        val cornerBottomRight = HumanBehaviorUtility.calculateHumanizedPoint(
            targetX = 1078f,
            targetY = 2398f,
            radiusPx = 25f,
            screenWidth = screenW,
            screenHeight = screenH
        )
        assertTrue("X must be inside screen", cornerBottomRight.x in 4f..screenW)
        assertTrue("Y must be inside screen", cornerBottomRight.y in 4f..screenH)
    }

    @Test
    fun computeVaryingDelay_producesVaryingValidDelays() {
        val baseDelay = 500L
        val delays = (1..30).map {
            HumanBehaviorUtility.computeVaryingDelay(
                baseDelayMs = baseDelay,
                variancePercentage = 20f,
                profile = HumanBehaviorUtility.MotorProfile.NATURAL
            )
        }

        delays.forEach { timing ->
            assertTrue("Total delay must be positive", timing.totalDelayMs > 15L)
            assertTrue("Press duration must be physiological (>30ms and <300ms)", timing.pressDurationMs in 30L..300L)
        }

        val distinctDelays = delays.map { it.totalDelayMs }.toSet()
        assertTrue("Delays should fluctuate stochastically", distinctDelays.size > 15)
    }

    @Test
    fun generateMultiTapSequence_generatesExpectedIntervals() {
        val intervals = HumanBehaviorUtility.generateMultiTapSequence(5, 100L)
        assertEquals(5, intervals.size)
        intervals.forEach { interval ->
            assertTrue("Multi-tap interval must be positive and reasonable", interval in 50L..350L)
        }
    }

    @Test
    fun computePosturalDrift_generatesSubtleShift() {
        val drift0 = HumanBehaviorUtility.computePosturalDrift(0)
        val drift10 = HumanBehaviorUtility.computePosturalDrift(10)
        assertNotNull(drift0)
        assertNotNull(drift10)
    }
}
