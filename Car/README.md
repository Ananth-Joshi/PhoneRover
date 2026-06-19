# 🚗 PhoneRover Onboard Controller (Android App)

This repository contains the native Android application that serves as the core "brain" and telemetry unit for the 4G-controlled RC car. The app runs on an onboard smartphone mounted directly to the vehicle chassis, handling high-speed video processing, low-latency WebRTC communications over cellular networks, and physical hardware control bridging.

---

## 🏗️ System Architecture & Data Flow

The app acts as the central middleware layer between the remote ground station controller and the physical steering/propulsion hardware.

```text
[ Remote Controller App ] 
         │  (WebRTC Data Channel / Low-latency Controls)
         ▼
[ Onboard Android App (This Repo) ]
         │  (USB Serial / UART at 9600 Baud)
         ▼
[ Arduino Nano Bridge ] 
         │  (PWM Signaling)
         ▼
[ L298N Motor Driver / SG90 Servo ]

```

---

## 🌟 Key Features

* **4G/LTE WebRTC Teleoperation:** Streams real-time **H.264/VP8 video** from the smartphone's rear camera to the ground station over cellular networks.
* **Low-Latency Command Processing:** Receives drive, reverse, and steering telemetry payloads via a secure WebRTC data channel.
* **Hardware Abstraction Bridge:** Interfaces directly with an onboard **Arduino Nano** via USB-OTG using raw serial communication to translate high-level software commands into physical movement.
* **Robust Network Fallbacks:** Automatically attempts ice-candidate gathering and TURN server relays to reliably bypass aggressive carrier Symmetric NAT firewalls.

---

## ⚙️ Prerequisites & Environment Setup

### 1. Hardware Requirements

* **Onboard Device:** Android smartphone running Android 7.0 (API 24) or higher (OTG support required).
* **USB OTG Adapter:** Type-C or Micro-USB to USB-A female adapter to bridge the phone to the microcontroller.
* **Microcontroller:** Arduino Nano (or compatible ATmega328P board).

### 2. Software Dependencies

* **Android Studio:** Jellyfish / Ladybug or newer.
* **Android SDK:** API 34 (Target SDK).
* **Gradle:** Version 8.0+

---

## 🚀 Getting Started

### 1. Clone and Open

Clone the repository and import the `/Car` directory directly into Android Studio.

```bash
git clone [https://github.com/Ananth-Joshi/phonerover.git](https://github.com/Ananth-Joshi/phonerover.git)
cd phonerover/Car

```

### 2. Configuration

Link your cloud servers using the local properties file.

1. Create or open the `local.properties` file in the car project root directory (`./local.properties`).
2. Add your server details to the bottom of the file:

```properties
SIGNALING_SERVER_URL="http://YOUR_SERVER_IP:3000"
TURN_SERVER_URI="turn:YOUR_SERVER_IP:3478"
TURN_USERNAME="your_turn_username"
TURN_PASSWORD="your_secure_password"

```

### 3. Deploy to Device

1. Enable **Developer Options** and **USB Debugging** on your target Android device.
2. Connect the phone to your development computer.
3. Click **Run** (`Shift + F10`) in Android Studio to compile and install the application.

---

## 🎛️ Serial Communication Protocol

The application serializes incoming driving commands and pipes them across the USB interface at **9600  Baud** using a concise, lightweight syntax to minimize packet overhead:

`A:[angle];S:[velocity]\n`

* **`angle`:** Integer value defining steering servo position.
* **`velocity`:** Integer value mapping directly to motor driver PWM cycles.
