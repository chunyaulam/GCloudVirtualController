# GCloud Virtual Controller

An Android virtual trackpad, remote keyboard, and gamepad controller application built with **Jetpack Compose**, **Material 3**, and **Kotlin Coroutines**.

Connected over a low-latency UDP socket protocol, GCloud Virtual Controller transforms your Android device (such as Logitech G Cloud, Android tablet, or phone) into an interactive remote controller for PC and Mac receiving servers.

---

## 🌟 Key Features

### 🖱️ Virtual Trackpad
- **1-Finger Tap**: Left Click
- **2-Finger Tap**: Right Click
- **2-Finger Swipe**: Smooth Sub-Pixel Scroll (X & Y axes)
- **Double-Tap & Hold**: Drag & Drop
- **3-Finger Tap**: Toggle Software Keyboard
- **Sub-pixel Motion Accumulation**: Silky smooth mouse movements and fine-grained scrolling.

### ⌨️ Remote Keyboard
- **Unicode Text Streaming**: Type full UTF-8 strings directly from your Android keyboard.
- **Control Keys Support**: Backspace / Delete, Enter / Return, Tab, and Escape.

### 🎮 Physical & Virtual Gamepad
- **Dual Analog Joysticks**: Left Stick (L3) & Right Stick (R3).
- **D-Pad**: Directional pad with multi-axis support.
- **Action Buttons**: ABXY face buttons, Select, and Start.
- **Shoulder & Triggers**: L1 / R1 bumpers & L2 / R2 analog pressure triggers.
- **Hardware Controller Pass-through**: Automatic detection and hot-plugging support for physical Bluetooth / USB gamepads.

### 💻 Device Management & Live Monitoring
- **Saved Endpoints**: Local Room database for managing target servers (IP & Port).
- **Concurrent Online Status Check**: Parallel async UDP pinging on screen launch and manual refresh.
- **Live Status Badges**: Visual indicators (🟢 Online / 🔴 Offline / ⏳ Checking...).
- **Offline Protection**: Blocks selecting offline servers with instant user toast feedback.

### 📡 Connection & Heartbeat Safety
- **UDP Heartbeat / Ping**: 2-second periodic heartbeat protocol.
- **Instant Failure Detection**: Catches ICMP `PortUnreachableException` and socket exceptions if the server process dies.
- **Auto-Disconnect Alert**: Prompts an alert dialog when server connection drops and gracefully terminates the session.

---

## 🖐️ Trackpad Gesture Quick Reference

| Gesture | Action |
| :--- | :--- |
| **1-Finger Tap** | Left Mouse Click |
| **2-Finger Tap** | Right Mouse Click |
| **2-Finger Swipe** | Vertical & Horizontal Scroll |
| **Double-Tap & Hold** | Mouse Drag & Drop |
| **3-Finger Tap** | Show / Hide Virtual Keyboard |

---

## 📡 UDP Protocol Specifications

Communication uses **UDP** packets sent to target port (Default: `8567`).

### 1. Ping / Heartbeat Packet (`0x00`)
- **Size**: 5 Bytes
- `Byte 0..3`: Timestamp (UInt32, Little-Endian)
- `Byte 4`: `0x00` (Packet Type)

### 2. Mouse Event Packet (`0x01`)
- **Size**: 16 Bytes
- `Byte 0..3`: Timestamp (UInt32, Little-Endian)
- `Byte 4`: `0x01` (Packet Type)
- `Byte 5`: Buttons Bitmask (`0x01`=Left, `0x02`=Right, `0x04`=Middle)
- `Byte 6..9`: Delta X (Float32, Little-Endian)
- `Byte 10..13`: Delta Y (Float32, Little-Endian)
- `Byte 14`: Scroll Y Delta (Int8, Range -127..127)
- `Byte 15`: Scroll X Delta (Int8, Range -127..127)

### 3. Keyboard Unicode Text Packet (`0x03`)
- **Size**: 1 + N Bytes
- `Byte 0`: `0x03` (Packet Type)
- `Byte 1..N`: UTF-8 Encoded String Bytes

### 4. Keyboard Control Key Packet (`0x04`)
- **Size**: 2 Bytes
- `Byte 0`: `0x04` (Packet Type)
- `Byte 1`: Control Key Action (`1` = Backspace, `2` = Enter, `3` = Tab, `4` = Escape)

### 5. Gamepad State Packet (`0x03`)
- **Size**: 17 Bytes
- `Byte 0..3`: Timestamp (UInt32, Little-Endian)
- `Byte 4`: `0x03` (Packet Type)
- `Byte 5..6`: Buttons Bitmask (UInt16, Little-Endian)
- `Byte 7..8`: Left Stick X (Int16, -32768..32767)
- `Byte 9..10`: Left Stick Y (Int16, -32768..32767)
- `Byte 11..12`: Right Stick X (Int16, -32768..32767)
- `Byte 13..14`: Right Stick Y (Int16, -32768..32767)
- `Byte 15`: Left Trigger Pressure (UInt8, 0..255)
- `Byte 16`: Right Trigger Pressure (UInt8, 0..255)

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose, Material 3
- **Asynchronous & Coroutines**: Kotlin Coroutines (`Dispatchers.IO`), Flow, StateFlow
- **Database**: Room Database with KSP (Kotlin Symbol Processing)
- **Networking**: Java `DatagramSocket` managed by background single-thread Executors (`UdpSenderManager`)
- **Architecture**: Single-Activity Compose Application (`MainActivity` → `DeviceSelectionScreen` / `TrackpadScreen`)

---

## 🚀 Building & Running

### Prerequisites
- **Android Studio**: Ladybug / 2024.2.1 or newer
- **JDK**: JDK 17
- **Android SDK**: Min SDK 26 (Android 8.0) / Target SDK 36

### Build Command
```bash
./gradlew assembleDebug
```

---

## 📄 License

This project is open-source and available under the **MIT License**.
