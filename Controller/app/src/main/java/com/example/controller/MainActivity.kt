package com.example.controller

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.controlwear.virtual.joystick.android.JoystickView
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.webrtc.IceCandidate
import org.webrtc.SurfaceViewRenderer
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory

class MainActivity : AppCompatActivity() {

    private lateinit var connectionBtn: Button
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var socket: Socket
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var joystick : JoystickView
    private lateinit var mapView : MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Hide the navigation and status bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }


        MapLibre.getInstance(this)  //This has to be called before setContentView
        setContentView(R.layout.activity_main)

        joystick = findViewById(R.id.joystick)
        connectionBtn = findViewById(R.id.btnConnect)
        remoteView = findViewById(R.id.remote_view)
        webRTCManager = WebRTCManager(this)
        mapView = findViewById<MapView>(R.id.mapView)


        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->

            // Hide default MapLibre UI elements
            map.uiSettings.apply {
                isLogoEnabled = false
                isAttributionEnabled = false
                isCompassEnabled = false
            }

            // Load the completely free vector style from OpenFreeMap
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->

                val startLocation = LatLng(0.0, 0.0, )
                val carBitmap = android.graphics.BitmapFactory.decodeResource(resources,R.drawable.car)

                style.addImage("rover-icon",carBitmap)

                val initialPoint = org.maplibre.geojson.Point.fromLngLat(0.0,0.0)
                val geoJsonSource = org.maplibre.android.style.sources.GeoJsonSource(
                    "rover-source",
                    org.maplibre.geojson.Feature.fromGeometry(initialPoint)
                )
                style.addSource(geoJsonSource)

                val symbolLayer = org.maplibre.android.style.layers.SymbolLayer("rover-layer", "rover-source")
                    .withProperties(
                        org.maplibre.android.style.layers.PropertyFactory.iconImage("rover-icon"),
                        org.maplibre.android.style.layers.PropertyFactory.iconSize(0.5f),
                        org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                        org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true)
                    )
                style.addLayer(symbolLayer)

                val position = CameraPosition.Builder()
                    .target(startLocation)
                    .zoom(18.5)
                    .tilt(0.0)
                    .bearing(0.0)
                    .build()

                // Move the camera once the style has successfully loaded
                map.moveCamera(CameraUpdateFactory.newCameraPosition(position))
            }
        }

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
            },
            onTelemetryReceived = { lat, lng, heading ->
                runOnUiThread {
                    mapView.getMapAsync { map ->
                        map.style?.let { style ->
                            val source = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("rover-source")
                            source?.setGeoJson(org.maplibre.geojson.Point.fromLngLat(lng, lat))

                            val layer = style.getLayerAs<org.maplibre.android.style.layers.SymbolLayer>("rover-layer")
                            layer?.setProperties(org.maplibre.android.style.layers.PropertyFactory.iconRotate(heading.toFloat()))

                            val position = CameraPosition.Builder()
                                .target(LatLng(lat, lng))
                                .build()
                            map.easeCamera(CameraUpdateFactory.newCameraPosition(position), 500)
                        }
                    }
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

        joystick.setOnMoveListener { angle, strength ->
            var servoAngle = 90
            var motorPWM = 0
            val mappedPWM = (strength * 2.55).toInt()

            if (angle in 0..180) {
                servoAngle = angle
                motorPWM = mappedPWM // Positive PWM for forward
            }
            else {
                servoAngle = 360 - angle
                motorPWM = -mappedPWM // Negative PWM for reverse
            }
            val jsonPayload = "{\"action\":\"drive\",\"pwm\":$motorPWM,\"servoAngle\":$servoAngle}"
            webRTCManager.sendDataThroughChannel(jsonPayload)
        }
    }
}