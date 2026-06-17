# PhoneRover Ground Station (Controller) 🎮

Android application acting as the primary control hub for the PhoneRover. Displays a live low-latency video feed, real-time GPS tracking, and translates physical joystick inputs into hardware-ready commands.

## 📖 Overview

This application serves as the "Smart Brain" of the PhoneRover architecture. It natively handles all polar coordinate math, reverse-drive mirroring, and variable speed (PWM) conversions. It then packages these exact hardware limits into a lightweight JSON payload and fires it across a 4G connection via WebRTC data channels.

## ✨ Features

* **Joystick:** Sends joystick input to car as angles and speed JSON.
* **Live Video Feed:** Real-time WebRTC video stream directly from the rover's onboard camera.
* **GPS Dashboard:** Live map integration tracking the rover's coordinates during surveillance.

## 📡 The JSON Payload

Every time the digital joystick moves, the app calculates the precise physical angles and speed limits required by the chassis and transmits them over the WebRTC data channel in the following format:

```json
{
  "action": "drive",
  "pwm": 255, 
  "servoAngle": 90
}

```

* **`pwm`**: `0 to 255` for forward drive, `-1 to -255` for reverse drive.
* **`servoAngle`**: `0 to 180` representing the physical servo degree mapping.

## 🏗️ Architecture

* **Language:** Kotlin
* **Communication:** WebRTC (Video + Data Channels)
* **UI Components:** Custom Joystick View, Video Surface View, Map Fragment

## 🚀 Setup & Installation

1. Open the project in Android Studio.
2. Ensure your WebRTC signaling server configuration (IP/Port) is correctly set in the `WebRTCManager` class.
3. Build and deploy to any Android 8.0+ device acting as the ground station.
