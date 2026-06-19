package com.example.phonerover_car

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import org.json.JSONObject
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.nio.ByteBuffer

class WebRTCManager (private val context: Context){
    private lateinit var peerConnectionFactory: PeerConnectionFactory;
    private var peerConnection: PeerConnection? = null
    val eglBase = org.webrtc.EglBase.create()
    var dataChannel: DataChannel? = null

    private var videoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var arduinoPort : UsbSerialPort? = null

    var lastSentAngle = ""
    var lastSentSpeed = ""

    init{
        var options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = org.webrtc.DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)

        peerConnectionFactory = PeerConnectionFactory
            .builder()
            .setVideoEncoderFactory(encoderFactory)
            .createPeerConnectionFactory();
    }

    fun createPeerConnection(
        onIceCandidateGenerated: (IceCandidate) -> Unit,
        onCommandReceived: (String) -> Unit,
        onWebRTCDisconnected: () -> Unit,
        onWebRTCConnected: () -> Unit
    ){
        val iceList = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),

            PeerConnection.IceServer.builder("turn:TURN_SERVER_IP:TURN_SERVER_PORT") 
                .setUsername("TURN_USERNAME")
                .setPassword("TURN_PASSWORD")
                .createIceServer()
        )

        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                println("APP LOG: P2P Connection State changed to: $newState")

                when (newState) {
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED -> {
                        println("APP LOG: Tunnel collapsed! Telling UI to hide.")
                        onWebRTCDisconnected()
                    }
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        println("APP LOG: 🟢 Connected and streaming!")
                        onWebRTCConnected()
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(p0: IceCandidate?) {
                p0?.let { candidate ->
                    println("APP LOG: Generated Local ICE Candidate")
                    onIceCandidateGenerated(candidate)
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {}

            override fun onAddStream(p0: MediaStream?) {}

            override fun onRemoveStream(p0: MediaStream?) {}

            override fun onDataChannel(incomingChannel: DataChannel?) {
                println("APP LOG: Steering DataChannel caught from Controller!")
                dataChannel = incomingChannel

                dataChannel?.registerObserver(object : DataChannel.Observer {
                    override fun onBufferedAmountChange(p0: Long) {}
                    override fun onStateChange() {
                        println("APP LOG: DataChannel state changed to: ${dataChannel?.state()}")
                    }

                    override fun onMessage(buffer: DataChannel.Buffer) {
                        val data = buffer.data
                        val bytes = ByteArray(data.remaining())
                        data.get(bytes)
                        val commandJSON = String(bytes, Charsets.UTF_8)
                        println("APP LOG: 🕹️ INCOMING COMMAND: $commandJSON")

                        try {
                            val json = JSONObject(commandJSON)

                            if (json.has("action") && json.getString("action") == "drive") {

                                val servoAngle = json.getInt("servoAngle")
                                val motorPWM = json.getInt("pwm")

                                val angleCommand = "A${servoAngle}\n"
                                val speedCommand = "S${motorPWM}\n"

                                if (angleCommand != lastSentAngle) {
                                    onCommandReceived(angleCommand)
                                    lastSentAngle = angleCommand
                                }

                                if (speedCommand != lastSentSpeed) {
                                    onCommandReceived(speedCommand)
                                    lastSentSpeed = speedCommand
                                }
                            }
                        } catch (e: Exception) {
                            println("APP LOG: 🛑 JSON Parse or USB Write Error - ${e.message}")
                        }

                    }
                })
            }

            override fun onRenegotiationNeeded() {}
        }

        peerConnection = peerConnectionFactory.createPeerConnection(iceList,observer)
    }

    fun handleRemoteOffer(sdpString: String, onAnswerReady : (String) -> Unit) {
        val sessionDescription = SessionDescription(
            SessionDescription.Type.OFFER,
            sdpString
        )
        peerConnection?.setRemoteDescription(object:SdpObserver{
            override fun onSetSuccess() {
                val constraints = MediaConstraints();
                peerConnection?.createAnswer(object:SdpObserver{
                    override fun onCreateSuccess(answerSdp: SessionDescription?) {
                        answerSdp?.let {
                            peerConnection?.setLocalDescription(object: SdpObserver{
                                override fun onSetSuccess() {
                                    onAnswerReady(it.description)
                                }
                                override fun onSetFailure(error: String?) { println("APP LOG: Local Answer Failed: $error") }
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onCreateFailure(p0: String?) {}
                            },it)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) { println("APP LOG: Create Answer Failed: $error") }
                    override fun onSetFailure(p0: String?) {}
                },constraints)
            }
            override fun onSetFailure(error: String?) { println("APP LOG: Remote Offer Failed: $error") }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        },sessionDescription)

    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
        println("APP LOG: Added Remote ICE Candidate from Controller")
    }

    private fun getRearCameraCapturer(context: Context): VideoCapturer?{
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        for(deviceName in deviceNames){
            if(enumerator.isBackFacing(deviceName)){
                println("Back Camera Found: $deviceName")
                return enumerator.createCapturer(deviceName,null)
            }
        }
        return null
    }

    fun startCameraAndAttachTrack(context: Context){
        val videoCapturer = getRearCameraCapturer(context)
        if (videoCapturer == null) {
            println("APP LOG: ❌ CRITICAL ERROR - No camera found!")
            return
        }

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext,)
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)

        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("car-video-track", videoSource)

        peerConnection?.addTrack(localVideoTrack)

        println("APP LOG: Camera track successfully attached to the PeerConnection!")
    }

    fun sendTelemetry(jsonString : String){
        if(dataChannel?.state()== DataChannel.State.OPEN){
            val bytes  = jsonString.toByteArray(Charsets.UTF_8)
            val buffer = ByteBuffer.wrap(bytes)
            dataChannel?.send(DataChannel.Buffer(buffer,false))
        }
    }
}