package com.example.kilivana_driver.ui.screens.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions

private const val MIN_SPEED_FOR_GPS_BEARING = 1.5f
private const val MIN_MOVE_METERS_FOR_HEADING = 10.0

internal class HeadingTracker {
    private var lastPoint: GeoPoint? = null
    private var heading = 0f

    fun update(
        fix: Location,
        fallbackTarget: GeoPoint? = null
    ): Float {
        val now = GeoPoint(fix.latitude, fix.longitude)
        val previous = lastPoint

        when {
            fix.hasBearing() && fix.speed >= MIN_SPEED_FOR_GPS_BEARING -> {
                heading = fix.bearing
                lastPoint = now
            }
            previous != null && previous.distanceToAsDouble(now) >= MIN_MOVE_METERS_FOR_HEADING -> {
                heading = previous.bearingTo(now).toFloat()
                lastPoint = now
            }
            previous == null -> {
                lastPoint = now
                if (fallbackTarget != null) {
                    heading = now.bearingTo(fallbackTarget).toFloat()
                }
            }
        }

        return heading
    }
}

internal class CarMarker {
    private var manager: PointAnnotationManager? = null
    private var annotation: PointAnnotation? = null
    private var currentTarget: Point = Point.fromLngLat(0.0, 0.0)
    private var shownBearing = 0f
    private val icon by lazy { createVehicleBitmap() }

    fun bind(manager: PointAnnotationManager) {
        if (this.manager === manager) return
        annotation?.let { this.manager?.delete(it) }
        annotation = null
        this.manager = manager
    }

    fun update(
        target: Point,
        bearing: Float
    ) {
        currentTarget = target
        shownBearing = normalize(bearing)

        val pointManager = manager ?: return
        val existing = annotation
        if (existing == null) {
            annotation = pointManager.create(
                PointAnnotationOptions()
                    .withPoint(currentTarget)
                    .withIconImage(icon)
                    .withIconSize(1.0)
                    .withIconRotate(shownBearing.toDouble())
            )
        } else {
            existing.point = currentTarget
            existing.iconRotate = shownBearing.toDouble()
            pointManager.update(existing)
        }
    }

    fun cancel() {
        annotation?.let { manager?.delete(it) }
        annotation = null
        currentTarget = Point.fromLngLat(0.0, 0.0)
    }

    private fun normalize(degrees: Float): Float {
        return ((degrees % 360f) + 360f) % 360f
    }

    fun currentTarget(): Point = currentTarget
    fun currentBearing(): Float = shownBearing

    private fun createVehicleBitmap(): Bitmap {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(59, 130, 246)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeJoin = Paint.Join.ROUND
        }

        canvas.drawCircle(size / 2f, size / 2f + 3f, 22f, shadowPaint)
        val arrow = Path().apply {
            moveTo(size / 2f, 9f)
            lineTo(51f, 51f)
            lineTo(size / 2f, 42f)
            lineTo(13f, 51f)
            close()
        }
        canvas.drawPath(arrow, fillPaint)
        canvas.drawPath(arrow, strokePaint)
        return bitmap
    }
}

internal class CurrentLocationMarker {
    private var manager: PointAnnotationManager? = null
    private var annotation: PointAnnotation? = null
    private val icon by lazy { createCurrentLocationBitmap() }

    fun bind(manager: PointAnnotationManager) {
        if (this.manager === manager) return
        annotation?.let { this.manager?.delete(it) }
        annotation = null
        this.manager = manager
    }

    fun update(target: Point) {
        val pointManager = manager ?: return
        val existing = annotation
        if (existing == null) {
            annotation = pointManager.create(
                PointAnnotationOptions()
                    .withPoint(target)
                    .withIconImage(icon)
                    .withIconSize(1.0)
            )
        } else {
            existing.point = target
            pointManager.update(existing)
        }
    }

    fun cancel() {
        annotation?.let { manager?.delete(it) }
        annotation = null
    }

    private fun createCurrentLocationBitmap(): Bitmap {
        val size = 72
        val center = size / 2f
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 59, 130, 246)
            style = Paint.Style.FILL
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(59, 130, 246)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        canvas.drawCircle(center, center, 28f, outerPaint)
        canvas.drawCircle(center, center, 13f, fillPaint)
        canvas.drawCircle(center, center, 13f, strokePaint)
        return bitmap
    }
}
