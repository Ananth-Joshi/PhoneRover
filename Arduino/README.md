# Arduino Motor Bridge 🌉

Arduino Nano sketch. Receives motor commands from the Android controller via USB serial and outputs physical PWM and PPM signals to the motor driver and steering servo.

## 📖 Overview

This sketch acts as the "dumb muscle" of the PhoneRover architecture. Because the tethered Android phone handles all complex math (like reverse-drive mirroring and 0-100 to 0-255 PWM conversion), this microcontroller runs an incredibly lightweight loop. It simply reads the serial buffer for specific character markers and instantly pushes those raw integer values to the hardware pins.

## ⚡ Hardware Pinout

| Component | Pin | Type | Function |
| --- | --- | --- | --- |
| **SG90 Servo** | `D9` | PWM (PPM) | Front Steering |
| **L298N IN1** | `D3` | PWM (`~`) | Motor A (Left) Forward |
| **L298N IN2** | `D5` | PWM (`~`) | Motor A (Left) Reverse |
| **L298N IN3** | `D6` | PWM (`~`) | Motor B (Right) Forward |
| **L298N IN4** | `D11` | PWM (`~`) | Motor B (Right) Reverse |

## 📡 Serial Protocol

The Arduino listens on baud rate `9600` for simple, newline-terminated string commands. The protocol uses a single-character prefix to route the incoming integer to the correct hardware component.

### Steering Commands: `'A'`

Controls the front servo angle.

* **Format:** `A[0-180]`
* **Example:** `A90` (Centers the wheels)
* **Example:** `A0` (Hard right lock)

### Drive Commands: `'S'`

Controls the dual rear DC motors. The code natively parses negative numbers to trigger the reverse `IN` pins.

* **Format:** `S[-255 to 255]`
* **Example:** `S255` (100% Forward Speed)
* **Example:** `S-127` (50% Reverse Speed)
* **Example:** `S0` (Full Stop)

## 🛠️ Dependencies

* **Built-in:** `Servo.h` (Standard Arduino library)

## 🚀 Installation & Setup

1. Ensure all four L298N `IN` wires are connected to PWM-capable (`~`) pins on the Nano to prevent violent, non-proportional acceleration.
2. Compile and upload via the Arduino IDE.
3. Connect to the Android "Brain" via a USB OTG cable.