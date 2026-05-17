package com.example.controller

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.github.controlwear.virtual.joystick.android.JoystickView
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SurfaceViewRenderer

class MainActivity : AppCompatActivity() {

    private lateinit var connectionBtn: Button
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var socket: Socket
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var joystick : JoystickView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joystick = findViewById(R.id.joystick)

        connectionBtn = findViewById(R.id.btnConnect)
        remoteView = findViewById(R.id.remote_view)


        webRTCManager = WebRTCManager(this)

        remoteView.init(webRTCManager.eglBase.eglBaseContext, null)
        remoteView.setMirror(false)
        remoteView.setEnableHardwareScaler(true)

        webRTCManager.createPeerConnection(
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
            onVideoTrackReceived = { videoTrack ->
                runOnUiThread {
                    println("APP LOG: Sinking video to screen...")
                    videoTrack.addSink(remoteView)
                }
            },
            onWebRTCDisconnected = {
                runOnUiThread {
                    remoteView.clearImage()
                    connectionBtn.visibility = android.view.View.VISIBLE
                    connectionBtn.text = "RECONNECT CAR"
//                    joystick.resetButtonPosition()
                    joystick.isEnabled = false
                    android.widget.Toast.makeText(this@MainActivity, "Car Disconnected!", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onWebRTCConnected = {
                runOnUiThread {
                    connectionBtn.visibility = android.view.View.GONE
                    joystick.isEnabled = true
//                    joystick.resetButtonPosition()
                }
            }
        )


        val roomId = "signalling-room"
        socket = IO.socket(BuildConfig.SIGNALING_SERVER_URL)
        socket.connect()

        socket.on(Socket.EVENT_CONNECT) {
            println("APP LOG: 🟢 Connected to Signaling Server!")
            socket.emit("join-room", roomId)
            runOnUiThread {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "✅ Server Reconnected!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                // Put the button back to normal
                connectionBtn.text = "CONNECT CAR"
                connectionBtn.isEnabled = true
            }
        }

        socket.on("answer") { args: Array<Any> ->
            try {
                val data = args[0] as JSONObject
                val sdpString = data.getString("sdp")
                println("APP LOG: Answer Received from Car!")
                webRTCManager.handleRemoteAnswer(sdpString)
            } catch (e: Exception) {
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

        socket.on(Socket.EVENT_DISCONNECT) {
            println("APP LOG: 🔌 Lost connection to the Node.js Signaling Server!")

            runOnUiThread {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "⚠️ Signaling Server Disconnected!",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                connectionBtn.text = "SERVER OFFLINE"
                connectionBtn.isEnabled = false
            }
        }

        connectionBtn.setOnClickListener {
            println("APP LOG: Generating Offer to call the car...")

            webRTCManager.createLocalOffer { generatedOffer ->

                val offerJSON = JSONObject()
                offerJSON.put("type", "offer")
                offerJSON.put("sdp", generatedOffer)

                val payload = JSONObject()
                payload.put("offer", offerJSON)
                payload.put("roomId", "signalling-room")

                socket.emit("offer", payload)
                println("APP LOG: Offer sent to server!")
            }
        }
        
        joystick.setOnMoveListener { angle, strength->
            val jsonPayload = "{\"action\":\"drive\",\"speed\":$strength,\"angle\":$angle}"
            webRTCManager.sendDataThroughChannel(jsonPayload)
        }
    }
}