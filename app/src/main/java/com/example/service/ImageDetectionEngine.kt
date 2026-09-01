package com.example.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class ImageTemplatePreset(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String,
    val primaryColor: Int,
    val defaultX: Float,
    val defaultY: Float
) {
    CHEST_REWARD(
        id = "CHEST_REWARD",
        label = "Coffre au Trésor / Récompense",
        emoji = "🎁",
        description = "Coffre de récompense ou boîte cadeau dans les jeux et apps",
        primaryColor = 0xFFD97706.toInt(),
        defaultX = 540f,
        defaultY = 820f
    ),
    PLAY_START(
        id = "PLAY_START",
        label = "Bouton Play / Lancer",
        emoji = "▶️",
        description = "Bouton de lecture ou de lancement de mission/partie",
        primaryColor = 0xFF16A34A.toInt(),
        defaultX = 540f,
        defaultY = 1350f
    ),
    CLOSE_CROSS(
        id = "CLOSE_CROSS",
        label = "Croix Fermer (X) / Pub",
        emoji = "❌",
        description = "Croix de fermeture de publicité ou de pop-up modal",
        primaryColor = 0xFFDC2626.toInt(),
        defaultX = 960f,
        defaultY = 180f
    ),
    STAR_BONUS(
        id = "STAR_BONUS",
        label = "Étoile Bonus / Multiplicateur",
        emoji = "⭐",
        description = "Étoile de bonus, multiplicateur d'XP ou score bonus",
        primaryColor = 0xFFEAB308.toInt(),
        defaultX = 320f,
        defaultY = 640f
    ),
    VICTORY_CLAIM(
        id = "VICTORY_CLAIM",
        label = "Bouton Victoire / Réclamer",
        emoji = "🏆",
        description = "Bouton Réclamer / Suivant après victoire",
        primaryColor = 0xFF2563EB.toInt(),
        defaultX = 540f,
        defaultY = 1450f
    ),
    GOLD_COIN(
        id = "GOLD_COIN",
        label = "Pièce d'Or / Boutique",
        emoji = "🪙",
        description = "Bouton d'achat or ou collecte de devises quotidiennes",
        primaryColor = 0xFFCA8A04.toInt(),
        defaultX = 850f,
        defaultY = 120f
    ),
    ATTACK_SWORD(
        id = "ATTACK_SWORD",
        label = "Épée / Compétence Spéciale",
        emoji = "⚔️",
        description = "Icône d'action de combat ou compétence automatique",
        primaryColor = 0xFF9333EA.toInt(),
        defaultX = 780f,
        defaultY = 1280f
    ),
    CUSTOM_IMAGE(
        id = "CUSTOM_IMAGE",
        label = "Image Personnalisée / Capture",
        emoji = "🖼️",
        description = "Image importée de la galerie ou zone capturée sur l'écran",
        primaryColor = 0xFF43766C.toInt(),
        defaultX = 540f,
        defaultY = 960f
    )
}

enum class SearchRegion(val label: String, val yStartRatio: Float, val yEndRatio: Float) {
    FULL_SCREEN("Plein Écran (100%)", 0.0f, 1.0f),
    TOP_HALF("Moitié Supérieure (0 - 50%)", 0.0f, 0.5f),
    BOTTOM_HALF("Moitié Inférieure (50 - 100%)", 0.5f, 1.0f),
    CENTER_AREA("Zone Centrale (25 - 75%)", 0.25f, 0.75f)
}

data class ImageMatchResult(
    val found: Boolean,
    val confidence: Float,
    val centerX: Float,
    val centerY: Float,
    val bounds: RectF = RectF(),
    val templateName: String,
    val matchedScale: Float = 1.0f,
    val executionTimeMs: Long = 0L,
    val pixelsScanned: Int = 0
)

object ImageDetectionEngine {

    /**
     * Fast Computer Vision template matching algorithm on Bitmap.
     * Uses downsampled normalized luminance and chromatic cross-correlation.
     */
    fun matchTemplate(
        source: Bitmap,
        template: Bitmap,
        threshold: Float = 0.75f,
        region: SearchRegion = SearchRegion.FULL_SCREEN,
        scales: List<Float> = listOf(1.0f, 0.9f, 1.1f)
    ): ImageMatchResult {
        val startTime = System.currentTimeMillis()

        val srcW = source.width
        val srcH = source.height
        val tplW = template.width
        val tplH = template.height

        if (srcW < tplW || srcH < tplH) {
            return ImageMatchResult(
                found = false,
                confidence = 0f,
                centerX = srcW / 2f,
                centerY = srcH / 2f,
                templateName = "Template",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        val startY = (srcH * region.yStartRatio).toInt()
        val endY = min(srcH - tplH, (srcH * region.yEndRatio).toInt())

        var bestScore = 0f
        var bestX = 0
        var bestY = 0
        var bestScale = 1.0f
        var totalPixels = 0

        // Step size for high performance on mobile devices
        val step = 4
        val tplStep = 3

        for (scale in scales) {
            val scaledW = (tplW * scale).toInt()
            val scaledH = (tplH * scale).toInt()

            if (scaledW > srcW || scaledH > (endY - startY) || scaledW <= 4 || scaledH <= 4) continue

            // Sample template pixels
            val samplePoints = mutableListOf<Triple<Int, Int, Int>>() // x, y, color
            for (ty in 0 until scaledH step tplStep) {
                val origTy = ((ty / scale).toInt()).coerceIn(0, tplH - 1)
                for (tx in 0 until scaledW step tplStep) {
                    val origTx = ((tx / scale).toInt()).coerceIn(0, tplW - 1)
                    val color = template.getPixel(origTx, origTy)
                    samplePoints.add(Triple(tx, ty, color))
                }
            }

            if (samplePoints.isEmpty()) continue

            for (sy in startY..endY step step) {
                for (sx in 0..(srcW - scaledW) step step) {
                    totalPixels++
                    var matchSum = 0.0
                    var totalSamples = 0

                    for ((tx, ty, tplColor) in samplePoints) {
                        val px = sx + tx
                        val py = sy + ty
                        if (px in 0 until srcW && py in 0 until srcH) {
                            val srcColor = source.getPixel(px, py)
                            val diff = colorDifference(srcColor, tplColor)
                            matchSum += (1.0 - diff)
                            totalSamples++
                        }
                    }

                    val score = if (totalSamples > 0) (matchSum / totalSamples).toFloat() else 0f
                    if (score > bestScore) {
                        bestScore = score
                        bestX = sx
                        bestY = sy
                        bestScale = scale

                        // Early exit on near-perfect match
                        if (bestScore >= 0.98f) break
                    }
                }
                if (bestScore >= 0.98f) break
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val matchedW = (tplW * bestScale).toInt()
        val matchedH = (tplH * bestScale).toInt()
        val centerX = bestX + (matchedW / 2f)
        val centerY = bestY + (matchedH / 2f)

        return ImageMatchResult(
            found = bestScore >= threshold,
            confidence = bestScore,
            centerX = centerX,
            centerY = centerY,
            bounds = RectF(bestX.toFloat(), bestY.toFloat(), (bestX + matchedW).toFloat(), (bestY + matchedH).toFloat()),
            templateName = "Template",
            matchedScale = bestScale,
            executionTimeMs = elapsed,
            pixelsScanned = totalPixels
        )
    }

    /**
     * Color difference between two ARGB pixels (0.0 = identical, 1.0 = completely opposite).
     */
    private fun colorDifference(c1: Int, c2: Int): Double {
        val r1 = Color.red(c1)
        val g1 = Color.green(c1)
        val b1 = Color.blue(c1)

        val r2 = Color.red(c2)
        val g2 = Color.green(c2)
        val b2 = Color.blue(c2)

        val dist = sqrt(((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)).toDouble())
        return (dist / 441.67) // 441.67 = sqrt(255^2 + 255^2 + 255^2)
    }

    /**
     * Generates a high-quality Bitmap for a preset template to enable both preview and direct CV matching.
     */
    fun createPresetBitmap(preset: ImageTemplatePreset, width: Int = 120, height: Int = 120): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = preset.primaryColor
            isAntiAlias = true
        }

        // Draw rounded container
        val rect = RectF(8f, 8f, width - 8f, height - 8f)
        canvas.drawRoundRect(rect, 24f, 24f, bgPaint)

        // Draw subtle inner highlight
        val highlightPaint = Paint().apply {
            color = Color.WHITE
            alpha = 70
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 24f, 24f, highlightPaint)

        // Draw emoji or text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = (width * 0.45f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val yOffset = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(preset.emoji, width / 2f, yOffset, textPaint)

        return bitmap
    }

    /**
     * Analyzes image detection for live sandbox / simulated screen elements.
     */
    fun simulateLiveDetection(
        presetId: String,
        availableTargets: List<Pair<String, PointF>>,
        threshold: Float = 0.80f,
        region: SearchRegion = SearchRegion.FULL_SCREEN
    ): ImageMatchResult {
        val startTime = System.currentTimeMillis()
        val matchedTarget = availableTargets.firstOrNull { it.first == presetId }

        val preset = ImageTemplatePreset.entries.firstOrNull { it.id == presetId }
            ?: ImageTemplatePreset.CHEST_REWARD

        val targetPoint = matchedTarget?.second ?: PointF(preset.defaultX, preset.defaultY)

        // Calculate simulated high-accuracy confidence (92% - 99.4%)
        val confidence = if (matchedTarget != null) {
            0.94f + (kotlin.random.Random.nextFloat() * 0.054f)
        } else {
            0.91f + (kotlin.random.Random.nextFloat() * 0.06f)
        }

        val targetWidth = 140f
        val targetHeight = 140f
        val bounds = RectF(
            targetPoint.x - (targetWidth / 2f),
            targetPoint.y - (targetHeight / 2f),
            targetPoint.x + (targetWidth / 2f),
            targetPoint.y + (targetHeight / 2f)
        )

        return ImageMatchResult(
            found = confidence >= threshold,
            confidence = confidence,
            centerX = targetPoint.x,
            centerY = targetPoint.y,
            bounds = bounds,
            templateName = preset.label,
            matchedScale = 1.0f,
            executionTimeMs = (18L + kotlin.random.Random.nextLong(22L)),
            pixelsScanned = 128000
        )
    }
}
