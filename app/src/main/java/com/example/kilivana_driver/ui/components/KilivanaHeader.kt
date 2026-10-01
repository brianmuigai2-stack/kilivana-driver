package com.example.kilivana_driver.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaWhite

/**
 * The app's green header band, shared by every top-level screen.
 *
 * The band deliberately paints *behind* the status bar instead of sitting below
 * it (the caller must not add its own statusBarsPadding) so the status bar
 * icons sit on green rather than on the page background, which is what makes
 * the bar read as part of the design instead of a system strip bolted on top.
 *
 * The band closes with rounded bottom corners and overlaps the content by a few
 * dp, so the page appears to slide underneath it rather than butting against
 * a flat rectangle.
 *
 * [HeaderMotif] is drawn in flat white at low alpha. Flat fills only — no
 * gradients anywhere in this header.
 */
@Composable
fun KilivanaHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KilivanaGreenCard)
    ) {
        HeaderMotif()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 8.dp, top = 10.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = KilivanaWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.size(6.dp))
            } else {
                Spacer(modifier = Modifier.size(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = KilivanaWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = KilivanaWhite.copy(alpha = 0.78f),
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onActionClick != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onActionClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = KilivanaWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Rounded bottom edge, filled with the page background so the content
        // below looks tucked underneath the band.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .size(18.dp)
                .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                .background(com.example.kilivana_driver.ui.theme.KilivanaBackground)
        )
    }
}

/**
 * Flat decorative motif for the green band: a set of concentric growth rings
 * opening from the right edge, plus a scatter of dots. It reads as a field or
 * a leaf vein without illustrating anything literally, and stays out of the
 * way because every stroke is white at low alpha.
 */
@Composable
private fun HeaderMotif() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.94f, size.height * 0.18f)
        val maxRadius = size.width * 0.62f
        val stroke = size.width * 0.012f

        // Growth rings, stepping outward. Drawn as a flat stroke, no fill, and
        // fading in alpha as they grow so the far ones don't crowd the title.
        for (step in 1..5) {
            val fraction = step / 5f
            drawCircle(
                color = Color.White.copy(alpha = 0.10f * (1.1f - fraction)),
                radius = maxRadius * fraction,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // A short arc hugging the bottom-left, echoing the band shape.
        drawArc(
            color = Color.White.copy(alpha = 0.07f),
            startAngle = 200f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.25f, size.height * 0.55f),
            size = Size(size.width * 0.5f, size.height * 0.9f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Sparse dots in the lower right, the field-sowing texture.
        val dots = listOf(
            Offset(0.80f, 0.72f) to 0.030f,
            Offset(0.88f, 0.62f) to 0.022f,
            Offset(0.71f, 0.86f) to 0.026f,
            Offset(0.92f, 0.82f) to 0.018f,
            Offset(0.63f, 0.63f) to 0.016f
        )
        dots.forEach { (fraction, dotRadius) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = size.width * dotRadius,
                center = Offset(size.width * fraction.x, size.height * fraction.y)
            )
        }
    }
}
