package com.singularitycode.gcloudvirtualcontroller.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singularitycode.gcloudvirtualcontroller.MouseRemoteController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TrackpadScreen(
    mouseController: MouseRemoteController?,
    isDarkTheme: Boolean,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current

    var backPressedOnce by remember { mutableStateOf(false) }

    BackHandler {
        if (backPressedOnce) {
            onBack()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    Scaffold { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color.Black else Color.White)
        ) {
            val paddingPx = with(density) { 16.dp.toPx() }
            val trackpadRect = Rect(
                left = paddingPx,
                top = paddingPx,
                right = constraints.maxWidth.toFloat() - paddingPx,
                bottom = constraints.maxHeight.toFloat() - paddingPx
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var lastTapUpTime = 0L
                        val dragDoubleTapTimeout = 200L

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!trackpadRect.contains(down.position)) return@awaitEachGesture

                            val downTime = System.currentTimeMillis()
                            val isDragCandidate = (downTime - lastTapUpTime) < dragDoubleTapTimeout

                            var maxPointers = 1
                            var cumulativeMovement = 0f
                            var isScrollLocked = false
                            var isDragging = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val activeChanges = event.changes.filter { it.pressed }
                                if (activeChanges.isEmpty()) break

                                maxPointers = maxOf(maxOf(maxPointers, event.changes.size), activeChanges.size)
                                val movement = event.changes.sumOf { it.positionChange().getDistance().toDouble() }.toFloat()
                                cumulativeMovement += movement

                                // If more than 1 finger, immediately cancel any left-click drag state
                                if (activeChanges.size > 1 && isDragging) {
                                    isDragging = false
                                    mouseController?.leftClick(false)
                                }

                                if (activeChanges.size == 2) {
                                    isScrollLocked = true
                                    val dy = activeChanges.map { it.positionChange().y }.average().toInt()
                                    val dx = activeChanges.map { it.positionChange().x }.average().toInt()
                                    if (dy != 0 || dx != 0) {
                                        mouseController?.scroll(
                                            deltaY = (-dy).coerceIn(-127, 127).toByte(),
                                            deltaX = (-dx).coerceIn(-127, 127).toByte()
                                        )
                                    }
                                } else if (activeChanges.size == 1 && !isScrollLocked) {
                                    // Handle Double Tap and Drag
                                    if (isDragCandidate && cumulativeMovement > 10f && !isDragging) {
                                        isDragging = true
                                        mouseController?.leftClick(true)
                                    }
                                    
                                    val change = activeChanges[0]
                                    val dragAmount = change.positionChange()
                                    if (dragAmount != Offset.Zero) {
                                        mouseController?.mouseMove(
                                            deltaX = dragAmount.x,
                                            deltaY = dragAmount.y
                                        )
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }

                            if (isDragging) {
                                mouseController?.leftClick(false)
                                lastTapUpTime = 0
                            } else if (cumulativeMovement < 10f) {
                                lastTapUpTime = System.currentTimeMillis()
                                scope.launch {
                                    // Only trigger tap actions if we didn't perform a long movement (swipe/scroll)
                                    when (maxPointers) {
                                        1 -> {
                                            mouseController?.leftClick(true)
                                            delay(50)
                                            mouseController?.leftClick(false)
                                        }
                                        2 -> {
                                            mouseController?.rightClick(true)
                                            delay(50)
                                            mouseController?.rightClick(false)
                                        }
                                    }
                                }
                            } else {
                                lastTapUpTime = 0
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val strokeColor = if (isDarkTheme) Color.DarkGray else Color.LightGray
                val textColor = if (isDarkTheme) Color.Gray else Color.DarkGray
                
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    drawRoundRect(
                        color = strokeColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    InstructionRow(Icons.Default.TouchApp, "1-Finger Tap: Left Click", textColor)
                    InstructionRow(Icons.Default.AdsClick, "2-Finger Tap: Right Click", textColor)
                    InstructionRow(Icons.Default.PanTool, "2-Finger Swipe: Scroll", textColor)
                    InstructionRow(Icons.Default.Mouse, "Double Tap & Hold: Drag", textColor)
                }
            }
        }
        val _ignore = padding
    }
}

@Composable
fun InstructionRow(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.width(240.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
