package com.singularitycode.gcloudvirtualcontroller.ui.screens

import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Keyboard
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singularitycode.gcloudvirtualcontroller.GamepadState
import com.singularitycode.gcloudvirtualcontroller.KeyboardRemoteController
import com.singularitycode.gcloudvirtualcontroller.MouseRemoteController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun TrackpadScreen(
    mouseController: MouseRemoteController?,
    keyboardController: KeyboardRemoteController?,
    isDarkTheme: Boolean,
    gamepadState: GamepadState,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(" ", selection = TextRange(1))) }

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
            delay(2.seconds)
            backPressedOnce = false
        }
    }

    Scaffold { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDarkTheme) Color.Black else Color.White)
        ) {
            val trackpadRect = remember(maxWidth, maxHeight, density) {
                with(density) {
                    val paddingPx = 16.dp.toPx()
                    Rect(
                        left = paddingPx,
                        top = paddingPx,
                        right = maxWidth.toPx() - paddingPx,
                        bottom = maxHeight.toPx() - paddingPx
                    )
                }
            }

            // Hidden TextField to capture keyboard events
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Avoid unnecessary updates if the value is the same
                    if (newValue.text == textFieldValue.text && newValue.selection == textFieldValue.selection) return@BasicTextField

                    val newText = newValue.text
                    if (newText.isEmpty()) {
                        // 長度變小，代表佔位符被刪除 -> Backspace
                        keyboardController?.sendControlKey(1)
                        textFieldValue = TextFieldValue(" ", selection = TextRange(1))
                    } else if (newText.length > 1) {
                        // 找出新增的文字
                        val addedText = when {
                            newText.startsWith(" ") -> newText.substring(1)
                            newText.endsWith(" ") -> newText.substring(0, newText.length - 1)
                            else -> newText.replace(" ", "")
                        }

                        if (addedText.isNotEmpty()) {
                            if (addedText == "\n") {
                                keyboardController?.sendControlKey(2)
                            } else {
                                keyboardController?.sendUnicodeText(addedText)
                            }
                        }
                        textFieldValue = TextFieldValue(" ", selection = TextRange(1))
                    } else {
                        textFieldValue = newValue
                    }
                },
                modifier = Modifier
                    .size(16.dp) // Slightly larger to help IME calculate cursor positions
                    .alpha(0f)  // Keep it invisible
                    .focusRequester(focusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

                        val controlKeyType = when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DEL -> 1    // Backspace
                            KeyEvent.KEYCODE_ENTER -> 2  // Enter
                            KeyEvent.KEYCODE_TAB -> 3    // Tab
                            KeyEvent.KEYCODE_ESCAPE -> 4 // Escape
                            else -> null
                        }

                        if (controlKeyType != null) {
                            // 通過特殊控制鍵通道發送 (0x04)
                            keyboardController?.sendControlKey(controlKeyType)
                            return@onKeyEvent true // 阻止事件繼續傳播
                        }

                        false
                    },
                singleLine = false, // 允許換行按鍵出現
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.None
                )
            )

            // Trackpad Overlay (Full Screen)
            TrackpadOverlay(
                mouseController = mouseController,
                trackpadRect = trackpadRect,
                isDarkTheme = isDarkTheme,
                scope = scope,
                onThreeFingerTap = {
                    isKeyboardVisible = !isKeyboardVisible
                    if (isKeyboardVisible) {
                        focusRequester.requestFocus()
                        softwareKeyboardController?.show()
                    } else {
                        softwareKeyboardController?.hide()
                    }
                }
            )

            // Gamepad Overlays
            if (gamepadState.isConnected) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        GamepadLeftPanel(gamepadState, isDarkTheme)
                    }

                    Spacer(modifier = Modifier.weight(1.2f))

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        GamepadRightPanel(gamepadState, isDarkTheme)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackpadOverlay(
    mouseController: MouseRemoteController?,
    trackpadRect: Rect,
    isDarkTheme: Boolean,
    scope: CoroutineScope,
    onThreeFingerTap: () -> Unit
) {
    val currentOnThreeFingerTap by rememberUpdatedState(onThreeFingerTap)
    val currentMouseController by rememberUpdatedState(mouseController)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(trackpadRect) {
                var lastTapUpUptime = 0L
                val dragDoubleTapTimeout = 200L
                var tapJob: Job? = null

                var scrollAccumulatorX = 0f
                var scrollAccumulatorY = 0f

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!trackpadRect.contains(down.position)) return@awaitEachGesture

                    val downUptime = SystemClock.uptimeMillis()
                    val isDragCandidate = (downUptime - lastTapUpUptime) < dragDoubleTapTimeout

                    var maxPointers = 1
                    var cumulativeMovement = 0f
                    var isScrollLocked = false
                    var isDragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val activeChanges = event.changes.filter { it.pressed }
                        if (activeChanges.isEmpty()) break

                        maxPointers = maxOf(maxPointers, event.changes.size)
                        val movement = event.changes.sumOf { it.positionChange().getDistance().toDouble() }.toFloat()
                        cumulativeMovement += movement

                        if (activeChanges.size > 1 && isDragging) {
                            isDragging = false
                            currentMouseController?.leftClick(false)
                        }

                        if (activeChanges.size == 2) {
                            isScrollLocked = true
                            val count = activeChanges.size.toFloat()
                            scrollAccumulatorY += activeChanges.sumOf { it.positionChange().y.toDouble() }.toFloat() / count
                            scrollAccumulatorX += activeChanges.sumOf { it.positionChange().x.toDouble() }.toFloat() / count

                            val sendY = scrollAccumulatorY.toInt()
                            val sendX = scrollAccumulatorX.toInt()

                            if (sendY != 0 || sendX != 0) {
                                scrollAccumulatorY -= sendY
                                scrollAccumulatorX -= sendX
                                currentMouseController?.scroll(
                                    deltaY = (-sendY).coerceIn(-127, 127).toByte(),
                                    deltaX = (-sendX).coerceIn(-127, 127).toByte()
                                )
                            }
                        } else if (activeChanges.size == 1 && !isScrollLocked) {
                            if (isDragCandidate && cumulativeMovement > 10f && !isDragging) {
                                isDragging = true
                                currentMouseController?.leftClick(true)
                            }

                            val change = activeChanges[0]
                            val dragAmount = change.positionChange()
                            if (dragAmount != Offset.Zero) {
                                currentMouseController?.mouseMove(
                                    deltaX = dragAmount.x,
                                    deltaY = dragAmount.y
                                )
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }

                    scrollAccumulatorX = 0f
                    scrollAccumulatorY = 0f

                    if (isDragging) {
                        currentMouseController?.leftClick(false)
                        lastTapUpUptime = 0L
                    } else if (cumulativeMovement < 10f) {
                        lastTapUpUptime = SystemClock.uptimeMillis()
                        tapJob?.cancel()
                        tapJob = scope.launch {
                            when (maxPointers) {
                                1 -> {
                                    currentMouseController?.leftClick(true)
                                    delay(50.milliseconds)
                                    currentMouseController?.leftClick(false)
                                }
                                2 -> {
                                    currentMouseController?.rightClick(true)
                                    delay(50.milliseconds)
                                    currentMouseController?.rightClick(false)
                                }
                                3 -> {
                                    currentOnThreeFingerTap()
                                }
                            }
                        }
                    } else {
                        lastTapUpUptime = 0L
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InstructionRow(Icons.Default.TouchApp, "1-Finger Tap: Left Click", textColor)
            InstructionRow(Icons.Default.AdsClick, "2-Finger Tap: Right Click", textColor)
            InstructionRow(Icons.Default.PanTool, "2-Finger Swipe: Scroll", textColor)
            InstructionRow(Icons.Default.Mouse, "Double Tap & Hold: Drag", textColor)
            InstructionRow(Icons.Default.Keyboard, "3-Finger Tap: Show/Hide Keyboard", textColor)
        }
    }
}

@Composable
fun GamepadLeftPanel(state: GamepadState, isDarkTheme: Boolean) {
    val tint = if (isDarkTheme) Color.White else Color.Black
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TriggerButton("L1", state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_L1), tint)
            TriggerButton("L2", state.l2 > 0.1f, tint, state.l2)
        }
        ButtonLabel("SELECT", state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_SELECT), tint)
        JoystickView("L-Stick", state.leftJoystick, state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_THUMBL), tint)
        DpadView(state.dpad, tint)
    }
}

@Composable
fun GamepadRightPanel(state: GamepadState, isDarkTheme: Boolean) {
    val tint = if (isDarkTheme) Color.White else Color.Black
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TriggerButton("R2", state.r2 > 0.1f, tint, state.r2)
            TriggerButton("R1", state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_R1), tint)
        }
        ButtonLabel("START", state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_START), tint)
        AbxyView(state.pressedButtons, tint)
        JoystickView("R-Stick", state.rightJoystick, state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_THUMBR), tint)
    }
}

@Composable
fun TriggerButton(label: String, isPressed: Boolean, tint: Color, pressure: Float = 0f) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isPressed) tint.copy(alpha = 0.4f + 0.6f * pressure) else tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ButtonLabel(label: String, isPressed: Boolean, tint: Color) {
    Text(
        text = label,
        color = if (isPressed) tint else tint.copy(alpha = 0.3f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun DpadView(dpad: Offset, tint: Color) {
    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(width = 80.dp, height = 24.dp).background(tint.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
        Box(modifier = Modifier.size(width = 24.dp, height = 80.dp).background(tint.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))

        if (dpad.y < -0.5f) Box(modifier = Modifier.size(24.dp).offset(y = (-28).dp).background(tint, RoundedCornerShape(4.dp)))
        if (dpad.y > 0.5f) Box(modifier = Modifier.size(24.dp).offset(y = 28.dp).background(tint, RoundedCornerShape(4.dp)))
        if (dpad.x < -0.5f) Box(modifier = Modifier.size(24.dp).offset(x = (-28).dp).background(tint, RoundedCornerShape(4.dp)))
        if (dpad.x > 0.5f) Box(modifier = Modifier.size(24.dp).offset(x = 28.dp).background(tint, RoundedCornerShape(4.dp)))
    }
}

@Composable
fun JoystickView(label: String, axis: Offset, isPressed: Boolean, tint: Color) {
    val density = LocalDensity.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(tint.copy(alpha = 0.05f), CircleShape)
                .border(1.dp, tint.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (axis.x * with(density) { 18.dp.toPx() }).roundToInt(),
                            (axis.y * with(density) { 18.dp.toPx() }).roundToInt()
                        )
                    }
                    .size(28.dp)
                    .background(if (isPressed) tint else tint.copy(alpha = 0.4f), CircleShape)
            )
        }
        Text(label, color = tint.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}

@Composable
fun AbxyView(pressedButtons: Set<Int>, tint: Color) {
    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
        GamepadButton("Y", KeyEvent.KEYCODE_BUTTON_Y in pressedButtons, Modifier.align(Alignment.TopCenter), tint)
        GamepadButton("A", KeyEvent.KEYCODE_BUTTON_A in pressedButtons, Modifier.align(Alignment.BottomCenter), tint)
        GamepadButton("X", KeyEvent.KEYCODE_BUTTON_X in pressedButtons, Modifier.align(Alignment.CenterStart), tint)
        GamepadButton("B", KeyEvent.KEYCODE_BUTTON_B in pressedButtons, Modifier.align(Alignment.CenterEnd), tint)
    }
}

@Composable
fun GamepadButton(label: String, isPressed: Boolean, modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(if (isPressed) tint else tint.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isPressed) Color.Transparent else tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InstructionRow(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(horizontal = 24.dp)
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
