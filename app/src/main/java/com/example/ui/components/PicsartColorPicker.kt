package com.example.ui.components

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.*

@Composable
fun PicsartColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // Hold local HSB state
    val hsv = remember(initialColor) {
        FloatArray(3).apply {
            colorToHSV(initialColor.toArgb(), this)
        }
    }

    var h by remember { mutableStateOf(hsv[0]) }
    var s by remember { mutableStateOf(hsv[1]) }
    var b by remember { mutableStateOf(hsv[2]) }

    val activeColor = remember(h, s, b) {
        Color(HSVToColor(floatArrayOf(h, s, b)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121212) // Pure Picsart Dark Theme
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Label
                Text(
                    text = "SPECIFY COLOR",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Top Bar split rectangle showing: Left (Old Color), Right (New Selected Color)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left: Old Color
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(initialColor)
                        )
                        // Right: New Color
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(activeColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Center Wheel with circular Hue ring and diamond-shaped saturation/brightness canvas
                BoxWithConstraints(
                    modifier = Modifier
                        .size(240.dp)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val sizePx = with(density) { maxWidth.toPx() }

                    // Compute dimensions
                    val center = Offset(sizePx / 2f, sizePx / 2f)
                    val rOuter = sizePx * 0.48f
                    val rInner = sizePx * 0.38f
                    val rSweep = (rOuter + rInner) / 2f
                    val rDiamondSpace = sizePx * 0.32f
                    val halfSide = rDiamondSpace * 0.70710678f

                    // Helper to identify and process drag zones
                    fun processTouch(offset: Offset) {
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val dist = sqrt(dx * dx + dy * dy)

                        if (dist >= rInner - 12f && dist <= rOuter + 12f) {
                            // Touch is on the Hue ring
                            var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angleDeg < 0f) angleDeg += 360f
                            h = angleDeg.coerceIn(0f, 360f)
                        } else if (dist < rDiamondSpace + 12f) {
                            // Touch is inside the Diamond
                            // Rotate by 45 degrees
                            val rx = (dx - dy) * 0.70710678f
                            val ry = (dx + dy) * 0.70710678f

                            // Clamp in rotated square
                            val rxClamped = rx.coerceIn(-halfSide, halfSide)
                            val ryClamped = ry.coerceIn(-halfSide, halfSide)

                            // S goes left-to-right diagonally
                            s = ((rxClamped + halfSide) / (2f * halfSide)).coerceIn(0f, 1f)
                            // B goes top-to-bottom diagonally
                            b = (1f - ((ryClamped + halfSide) / (2f * halfSide))).coerceIn(0f, 1f)
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { startOffset -> processTouch(startOffset) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        processTouch(change.position)
                                    }
                                )
                            }
                    ) {
                        // A. Draw Outer circular Hue spectrum ring
                        val hueColors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                            Color.Blue, Color.Magenta, Color.Red
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(hueColors, center),
                            radius = rSweep,
                            style = Stroke(width = rOuter - rInner)
                        )

                        // B. Draw Selected Hue Indicator ring
                        val hueAngleRad = Math.toRadians(h.toDouble())
                        val hueMarkerX = center.x + rSweep * cos(hueAngleRad).toFloat()
                        val hueMarkerY = center.y + rSweep * sin(hueAngleRad).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(hueMarkerX, hueMarkerY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 4.dp.toPx(),
                            center = Offset(hueMarkerX, hueMarkerY),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // C. Draw Diamond-shaped Saturation and Brightness canvas
                        // We do this by applying a 45-degree rotation to draw a perfectly centered square
                        withTransform({
                            rotate(degrees = 45f, pivot = center)
                        }) {
                            // Draw underlying horizontal gradient (White on Left, Hue on Right)
                            val hueBaseColor = Color(HSVToColor(floatArrayOf(h, 1f, 1f)))
                            val horizBrush = Brush.horizontalGradient(
                                colors = listOf(Color.White, hueBaseColor),
                                startX = center.x - halfSide,
                                endX = center.x + halfSide
                            )
                            drawRect(
                                brush = horizBrush,
                                topLeft = Offset(center.x - halfSide, center.y - halfSide),
                                size = androidx.compose.ui.geometry.Size(halfSide * 2, halfSide * 2)
                            )

                            // Overlay vertical gradient (Transparent on Top, Black on Bottom)
                            val vertBrush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startY = center.y - halfSide,
                                endY = center.y + halfSide
                            )
                            drawRect(
                                brush = vertBrush,
                                topLeft = Offset(center.x - halfSide, center.y - halfSide),
                                size = androidx.compose.ui.geometry.Size(halfSide * 2, halfSide * 2)
                            )

                            // Outline the square rotated at 45 degrees
                            drawRect(
                                color = Color.White.copy(alpha = 0.2f),
                                topLeft = Offset(center.x - halfSide, center.y - halfSide),
                                size = androidx.compose.ui.geometry.Size(halfSide * 2, halfSide * 2),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // D. Draw Saturation & Brightness Selector Marker inside Diamond
                        // We translate S & B from the rotated system back to normal view offsets
                        val rxMarker = s * (2f * halfSide) - halfSide
                        val ryMarker = (1f - b) * (2f * halfSide) - halfSide

                        // Map rotated point back
                        val markerDx = rxMarker * 0.70710678f + ryMarker * 0.70710678f
                        val markerDy = -rxMarker * 0.70710678f + ryMarker * 0.70710678f

                        val markerPos = Offset(center.x + markerDx, center.y + markerDy)

                        // Draw picker selector circle
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = markerPos,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 4.dp.toPx(),
                            center = markerPos,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Bottom Sliders for H, S, and B
                // A. Hue Slider (H)
                PicsartSliderRow(
                    label = "H",
                    value = h.toInt(),
                    range = 0f..360f,
                    trackBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                            Color.Blue, Color.Magenta, Color.Red
                        )
                    ),
                    onValueChange = { h = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // B. Saturation Slider (S)
                val satStartColor = Color(HSVToColor(floatArrayOf(h, 0f, b)))
                val satEndColor = Color(HSVToColor(floatArrayOf(h, 1f, b)))
                PicsartSliderRow(
                    label = "S",
                    value = (s * 100).toInt(),
                    range = 0f..100f,
                    trackBrush = Brush.horizontalGradient(
                        colors = listOf(satStartColor, satEndColor)
                    ),
                    onValueChange = { s = it / 100f }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // C. Brightness Slider (B)
                val brightStartColor = Color(HSVToColor(floatArrayOf(h, s, 0f)))
                val brightEndColor = Color(HSVToColor(floatArrayOf(h, s, 1f)))
                PicsartSliderRow(
                    label = "B",
                    value = (b * 100).toInt(),
                    range = 0f..100f,
                    trackBrush = Brush.horizontalGradient(
                        colors = listOf(brightStartColor, brightEndColor)
                    ),
                    onValueChange = { b = it / 100f }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        border = borderStroke()
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    }

                    Button(
                        onClick = {
                            onColorSelected(activeColor)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColor,
                            contentColor = if (b > 0.6f && s < 0.3f) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Select", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun borderStroke() = ButtonDefaults.outlinedButtonColors().run {
    androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
}

@Composable
fun PicsartSliderRow(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    trackBrush: Brush,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(20.dp)
        )

        // Custom Slider Track to display the gradient accurately
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background gradient track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(trackBrush)
            )

            // Compose standard Slider on top of the custom gradient track
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Live Integer Value
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
