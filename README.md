# PhoneRover 🚗📱

A 4G connected surveillance rover built from a repurposed smartphone. Streams live video, GPS tags detections, and can be controlled from anywhere in the world.

## 📖 Overview

PhoneRover is a low-cost, high-capability teleoperation project. By leveraging the massive processing power, camera, and LTE capabilities of a retired Android smartphone (Redmi Note 5 Pro), this project completely bypasses the need for expensive radio transmitters and custom processing boards like the Raspberry Pi.

The architecture follows a strict **"Smart Brain, Dumb Muscle"** pattern. The Android ecosystem handles all complex math, video streaming, and networking, while a tethered Arduino acts strictly as a hardware relay to drive the physical motors.

## ✨ Features

* **Global Telemetry & Control:** Drive the rover from anywhere with an internet connection via low-latency WebRTC data channels.
* **Proportional Speed & Steering:** Full PWM motor control mapping and precise servo articulation driven by a digital joystick interface.
* **Live Video Feed:** Real-time visual feedback straight from the onboard smartphone camera.
* **Modular "Smart Brain" Architecture:** The Android app computes all polar coordinate math, reverse mirroring, and PWM conversions, allowing the microcontroller to run incredibly lightweight code.

## 🛠️ Hardware Stack

* **Core Brain:** Android Smartphone (Redmi Note 5 Pro)
* **Microcontroller:** Arduino Nano
* **Motor Driver:** L298N Dual H-Bridge
* **Steering:** SG90 Micro Servo
* **Drive:** 2x Standard Yellow DC Gear Motors
* **Power:** 3x 18650 Lithium-Ion Batteries (Split between Logic/Steering and Drive components)
* **Link:** USB OTG Cable (Phone to Arduino Serial)

## 💻 Software Stack

* **Controller App (Android/Kotlin):** Captures joystick input, calculates hardware-ready angles and PWM values, and transmits a JSON payload over WebRTC.
* **Rover App (Android/Kotlin):** Receives the WebRTC signal, parses the JSON, and pushes raw string commands down the USB OTG connection.
* **Rover Firmware (C++):** Listens to the Serial port, catches `A` (Angle) and `S` (Speed) markers, and directly manipulates the hardware pins.

## 🧠 System Architecture

```text
[Controller Phone] 
       |
  (Calculates Math & JSON)
       |
   [WebRTC 4G] 
       |
[Onboard PhoneRover App]
       |
  (USB OTG Serial)
       |
 [Arduino Nano] ---> [SG90 Servo] (Steering)
       |
  [L298N Driver] ---> [DC Motors] (Drive)

```

## 🌍 4G/LTE Teleoperation (Beyond Line-of-Sight)

PhoneRover is built to drive anywhere. While it works perfectly over local Wi-Fi for testing, the true goal of this project is long-distance teleoperation over a cellular network.

Because cellular providers use strict firewalls and CGNAT, you cannot connect to the car's WebRTC video stream directly over 4G. To punch through these networks and maintain ultra-low latency steering, PhoneRover uses a custom TURN server.

We have included a fully Dockerized, secure Coturn infrastructure setup that you can deploy to a free cloud provider (like Oracle Cloud) in about 5 minutes.

👉 **[Click here for the 4G Cloud Infrastructure Guide]("./TURN Server/README.md")**
