package com.example.phonerover_car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

class ArduinoUsbManager(private val context: Context) {

    private var arduinoPort: UsbSerialPort? = null
    private var isReceiverRegistered = false

    // The BroadcastReceiver lives safely inside this class
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    println("APP LOG: 🔌 Hardware Alert: USB Device Attached!")
                    // Give the hardware 200ms to mount before trying to open the port
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        connectUsb()
                    }, 200)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    println("APP LOG: ❌ Hardware Alert: USB Device Detached!")
                    disconnectUsb()
                }
            }
        }
    }

    fun startListening() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter)
        isReceiverRegistered = true

        // Try an initial connection in case it's already plugged in
        connectUsb()
    }

    fun stopListening() {
        if (isReceiverRegistered) {
            context.unregisterReceiver(usbReceiver)
            isReceiverRegistered = false
        }
        disconnectUsb()
    }

    private fun connectUsb() {
        if (arduinoPort != null && arduinoPort!!.isOpen) {
            println("APP LOG: USB already connected and open.")
            return
        }

        try {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)

            if (availableDrivers.isNotEmpty()) {
                val driver = availableDrivers[0]
                val connection = manager.openDevice(driver.device)

                if (connection != null) {
                    arduinoPort = driver.ports[0]
                    arduinoPort?.open(connection)
                    arduinoPort?.setParameters(
                        9600,
                        8,
                        UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE
                    )
                    arduinoPort?.dtr = true
                    arduinoPort?.rts = true
                    println("APP LOG: 🔌 Arduino Connected Successfully via USB OTG!")
                } else {
                    println("APP LOG: 🛑 USB Connection failed! (Permission missing?)")
                }
            } else {
                println("APP LOG: 🛑 No Arduino found during scan.")
            }
        } catch (e: Exception) {
            println("APP LOG: 🛑 USB Crash: ${e.message}")
        }
    }

    private fun disconnectUsb() {
        try {
            if (arduinoPort != null && arduinoPort!!.isOpen) {
                arduinoPort?.close()
                println("APP LOG: 🔌 USB Disconnected and port closed safely.")
            }
        } catch (e: Exception) {
            println("APP LOG: Error closing USB port: ${e.message}")
        } finally {
            arduinoPort = null
        }
    }

    // Expose a public function to send commands directly to the hardware
    fun sendCommand(command: String) {
        arduinoPort?.let { port ->
            if (port.isOpen) {
                try {
                    val payload = "$command\n".toByteArray(Charsets.UTF_8)
                    port.write(payload, 100)
                } catch (e: Exception) {
                    println("APP LOG: Error writing to USB - ${e.message}")
                }
            }
        }
    }
}