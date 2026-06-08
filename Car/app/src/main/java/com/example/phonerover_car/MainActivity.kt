package com.example.phonerover_car

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.IceCandidate

class MainActivity : AppCompatActivity() {
    private lateinit var socket: Socket
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var usbManager: ArduinoUsbManager

    private lateinit var telemetryEngine: TelemetryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = ArduinoUsbManager(this)
        usbManager.startListening()

        val roomId = "signalling-room"
        socket = IO.socket(BuildConfig.SIGNALING_SERVER_URL)
        socket.connect()

        telemetryEngine = TelemetryEngine(this){
            telemetryJson ->
//                println("APP LOG: telemetryJSON $telemetryJson")
                webRTCManager.sendTelemetry(telemetryJson)
        }

        webRTCManager = WebRTCManager(this)
        webRTCManager.createPeerConnection(
            onCommandReceived = { command ->
                // 3. The magic link: WebRTC gets a command, tell the USB to send it
                usbManager.sendCommand(command)
            },
            onIceCandidateGenerated = { candidate ->
                val candidateJson = JSONObject()
                candidateJson.put("candidate", candidate.sdp)
                candidateJson.put("sdpMid", candidate.sdpMid)
                candidateJson.put("sdpMLineIndex", candidate.sdpMLineIndex)

                val payload = JSONObject()
                payload.put("icecandidate", candidateJson)
                payload.put("roomId", "signalling-room")

                socket.emit("ice-candidate", payload)
                println("APP LOG: Sent Android ICE Candidate to server")
            },
            onWebRTCConnected = {
                println("APP LOG: Webrtc Connected")
            },
            onWebRTCDisconnected = {
                println("APP LOG: Webrtc Connected")
            }
        )

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
            println("APP LOG: 🛑 Camera permission missing! Asking user...")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            println("APP LOG: 📸 Camera permission granted! Starting video track.")
            webRTCManager.startCameraAndAttachTrack(this)
            telemetryEngine.startTracking()
        }

        socket.on(Socket.EVENT_CONNECT){
            println("connected to signalling server")
            socket.emit("join-room", roomId)
        }


        socket.on("offer"){ args: Array<Any> ->
            try{
                val data = args[0] as JSONObject
                val sdpString = data.getString("sdp")
                println("APP LOG: Offer Received from Controller!")
                webRTCManager.handleRemoteOffer(sdpString){ answerString ->
                    val answerJson = JSONObject().apply {
                        put("sdp",answerString)
                        put("type","answer")
                        put("roomId","signalling-room")
                    }
                    socket.emit("answer",answerJson)
                }
            }catch (e: Exception){
                println("APP LOG: JSON Parsing Error - ${e.message}")
            }
        }

        socket.on("ice-candidate") { args: Array<Any> ->
            try {
                val data = args[0] as JSONObject
                val iceObject = if (data.has("icecandidate")) {
                    data.getJSONObject("icecandidate")
                } else {
                    data
                }

                val sdpMid = iceObject.getString("sdpMid")
                val sdpMLineIndex = iceObject.getInt("sdpMLineIndex")
                val sdp = iceObject.getString("candidate")

                if (sdp.isNotEmpty()) {
                    val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                    webRTCManager.addRemoteIceCandidate(iceCandidate)
                }

            } catch (e: Exception) {
                println("APP LOG: ICE parsing error - ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop listening for USB events to prevent memory leaks when the app closes
        usbManager.stopListening()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            // Verify that the user actually granted all the requested permissions
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                println("APP LOG: 📸 Permissions granted from popup! Starting video track & telemetry.")

                webRTCManager.startCameraAndAttachTrack(this)
                telemetryEngine.startTracking()
            } else {
                println("APP LOG: 🛑 User denied permissions. Cannot drive the RC car.")
            }
        }
    }
}