package com.example.kilivana_driver.ui.screens.map

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.view.animation.LinearInterpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.abs

private const val MIN_SPEED_FOR_GPS_BEARING = 1.5f
private const val MIN_MOVE_METERS_FOR_HEADING = 10.0
private const val MIN_MOVE_ANIMATION_MS = 800L
private const val MAX_MOVE_ANIMATION_MS = 3_000L
private const val REDRAW_ANGLE_DEGREES = 4f

internal class HeadingTracker {

    private var lastPoint: GeoPoint? = null
    private var heading = 0f

    fun update(
        fix: Location,
        fallbackTarget: GeoPoint? = null
    ): Float {

        val now = GeoPoint(
            fix.latitude,
            fix.longitude
        )

        val previous = lastPoint

        when {
            fix.hasBearing() &&
                fix.speed >= MIN_SPEED_FOR_GPS_BEARING -> {

                heading = fix.bearing
                lastPoint = now
            }

            previous != null &&
                previous.distanceToAsDouble(now) >= MIN_MOVE_METERS_FOR_HEADING -> {

                heading = previous.bearingTo(now).toFloat()
                lastPoint = now
            }

            previous == null -> {

                lastPoint = now

                if (fallbackTarget != null) {
                    heading = now.bearingTo(
                        fallbackTarget
                    ).toFloat()
                }
            }
        }

        return heading
    }
}

internal class CarMarker(
    private val mapView: MapView,
    context: Context
) {

    private val resources = context.resources

    private val baseBitmap: Bitmap =
        drawCarBitmap(context)

    val marker: Marker =
        Marker(mapView).apply {

            setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_CENTER
            )

            setOnMarkerClickListener(
                Marker.OnMarkerClickListener { _, _ ->
                    true
                }
            )
        }

    private var animator: ValueAnimator? = null
    private var hasPosition = false
    private var shownBearing = 0f
    private var drawnBearing = Float.NaN
    private var lastUpdateTime = 0L

    init {
        applyBearing(0f)
    }

    fun update(
        target: GeoPoint,
        bearing: Float
    ) {

        val now = System.currentTimeMillis()
        val normalizedTarget = normalize(bearing)

        if (!hasPosition) {

            hasPosition = true
            lastUpdateTime = now

            marker.position = target

            applyBearing(normalizedTarget)

            mapView.invalidate()

            return
        }

        val duration =
            (now - lastUpdateTime)
                .coerceIn(
                    MIN_MOVE_ANIMATION_MS,
                    MAX_MOVE_ANIMATION_MS
                )

        lastUpdateTime = now

        animator?.cancel()

        val fromLat = marker.position.latitude
        val fromLon = marker.position.longitude
        val fromBearing = shownBearing

        val turn =
            (normalizedTarget - fromBearing + 540f) %
                360f - 180f

        animator =
            ValueAnimator.ofFloat(0f, 1f).apply {

                this.duration = duration
                interpolator = LinearInterpolator()

                addUpdateListener { animation ->

                    val progress =
                        animation.animatedValue as Float

                    marker.position =
                        GeoPoint(
                            fromLat +
                                (target.latitude - fromLat) *
                                progress,

                            fromLon +
                                (target.longitude - fromLon) *
                                progress
                        )

                    applyBearing(
                        fromBearing +
                            turn * progress
                    )

                    mapView.invalidate()
                }

                start()
            }
    }

    fun cancel() {

        animator?.cancel()
        animator = null
    }

    private fun normalize(
        degrees: Float
    ): Float {

        return (
            (degrees % 360f) + 360f
        ) % 360f
    }

    private fun applyBearing(
        bearing: Float
    ) {

        val normalized =
            normalize(bearing)

        shownBearing = normalized

        if (
            !drawnBearing.isNaN() &&
            angleDifference(
                drawnBearing,
                normalized
            ) < REDRAW_ANGLE_DEGREES
        ) {
            return
        }

        drawnBearing = normalized

        val size = baseBitmap.width

        val rotated =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(rotated)

        canvas.rotate(
            normalized,
            size / 2f,
            size / 2f
        )

        canvas.drawBitmap(
            baseBitmap,
            0f,
            0f,
            null
        )

        marker.icon =
            BitmapDrawable(
                resources,
                rotated
            )
    }

    private fun angleDifference(
        a: Float,
        b: Float
    ): Float {

        return abs(
            (a - b + 540f) %
                360f - 180f
        )
    }
}

private fun drawCarBitmap(
    context: Context
): Bitmap {

    val size =
        (
            56 *
                context.resources.displayMetrics.density
        )
            .toInt()
            .coerceAtLeast(64)

    val s = size.toFloat()

    val bitmap =
        Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

    val canvas = Canvas(bitmap)

    val cx = s / 2f

    val bodyWidth = 0.46f * s
    val bodyLength = 0.86f * s

    val top = (s - bodyLength) / 2f
    val left = cx - bodyWidth / 2f

    val body =
        RectF(
            left,
            top,
            left + bodyWidth,
            top + bodyLength
        )

    fun paint(argb: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb
        }

    canvas.drawRoundRect(
        RectF(
            body.left - 0.02f * s,
            body.top + 0.03f * s,
            body.right + 0.02f * s,
            body.bottom + 0.03f * s
        ),
        0.18f * s,
        0.18f * s,
        paint(0x33000000)
    )

    val wheelPaint =
        paint(0xFF1F2937.toInt())

    val wheelWidth = 0.05f * s
    val wheelHeight = 0.15f * s

    for (fraction in listOf(0.2f, 0.74f)) {

        val wheelTop =
            top + bodyLength * fraction

        canvas.drawRoundRect(
            RectF(
                body.left - wheelWidth * 0.6f,
                wheelTop,
                body.left + wheelWidth * 0.4f,
                wheelTop + wheelHeight
            ),
            3f,
            3f,
            wheelPaint
        )

        canvas.drawRoundRect(
            RectF(
                body.right - wheelWidth * 0.4f,
                wheelTop,
                body.right + wheelWidth * 0.6f,
                wheelTop + wheelHeight
            ),
            3f,
            3f,
            wheelPaint
        )
    }

    canvas.drawRoundRect(
        body,
        0.17f * s,
        0.17f * s,
        paint(0xFFFFFFFF.toInt())
    )

    val inset = 0.03f * s

    val inner =
        RectF(
            body.left + inset,
            body.top + inset,
            body.right - inset,
            body.bottom - inset
        )

    canvas.drawRoundRect(
        inner,
        0.14f * s,
        0.14f * s,
        paint(0xFF3B82F6.toInt())
    )

    val glass =
        paint(0xFF1E293B.toInt())

    val glassWidth = 0.34f * s

    fun glassBand(
        fromFraction: Float,
        toFraction: Float
    ) =
        RectF(
            cx - glassWidth / 2f,
            top + bodyLength * fromFraction,
            cx + glassWidth / 2f,
            top + bodyLength * toFraction
        )

    canvas.drawRoundRect(
        glassBand(0.20f, 0.38f),
        0.05f * s,
        0.05f * s,
        glass
    )

    canvas.drawRoundRect(
        glassBand(0.40f, 0.62f),
        0.03f * s,
        0.03f * s,
        paint(0xFF60A5FA.toInt())
    )

    canvas.drawRoundRect(
        glassBand(0.64f, 0.78f),
        0.04f * s,
        0.04f * s,
        glass
    )

    val headlight =
        paint(0xFFFFF59D.toInt())

    canvas.drawCircle(
        cx - 0.13f * s,
        top + 0.045f * bodyLength + inset,
        0.035f * s,
        headlight
    )

    canvas.drawCircle(
        cx + 0.13f * s,
        top + 0.045f * bodyLength + inset,
        0.035f * s,
        headlight
    )

    val taillight =
        paint(0xFFEF4444.toInt())

    canvas.drawCircle(
        cx - 0.13f * s,
        top + bodyLength - 0.045f * bodyLength - inset,
        0.03f * s,
        taillight
    )

    canvas.drawCircle(
        cx + 0.13f * s,
        top + bodyLength - 0.045f * bodyLength - inset,
        0.03f * s,
        taillight
    )

    return bitmap
}
