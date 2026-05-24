package com.example.controller

import android.content.Context
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import java.nio.ByteBuffer

class WebRTCManager(private val context: Context) {
    private lateinit var peerConnectionFactory: PeerConnectionFactory;
    private var peerConnection: PeerConnection? = null
    val eglBase = org.webrtc.EglBase.create()
    var dataChannel: DataChannel? = null

    init {
        var options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val decoderFactory = org.webrtc.DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        val encoderFactory = org.webrtc.DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)

        peerConnectionFactory = PeerConnectionFactory
            .builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory();
    }

    fun createPeerConnection(
        onIceCandidateGenerated: (IceCandidate) -> Unit,
        onVideoTrackReceived: (org.webrtc.VideoTrack) -> Unit,
        onWebRTCDisconnected: () -> Unit,
        onWebRTCConnected: () -> Unit
    )  {
        val iceList = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
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

            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()

                if (track is org.webrtc.VideoTrack) {
                    println("APP LOG: Video Track Received from Car!")

                    onVideoTrackReceived(track)
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                p0?.let { candidate ->
                    println("APP LOG: Generated Local ICE Candidate")
                    onIceCandidateGenerated(candidate)
                }
            }


            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        }

        peerConnection = peerConnectionFactory.createPeerConnection(iceList, observer)
        peerConnection?.addTransceiver(
            org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            org.webrtc.RtpTransceiver.RtpTransceiverInit(org.webrtc.RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )

        val dcInit = DataChannel.Init().apply {
            ordered = false
            maxRetransmits = 0
        }

        dataChannel = peerConnection?.createDataChannel("rover-data-stream",dcInit)
    }

    fun createLocalOffer(onOfferReady: (String) -> Unit) {
        val constraints = org.webrtc.MediaConstraints()
        constraints.mandatory.add(org.webrtc.MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        constraints.mandatory.add(org.webrtc.MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))

        peerConnection?.createOffer(object : org.webrtc.SdpObserver {
            override fun onCreateSuccess(sessionDescription: org.webrtc.SessionDescription?) {
                sessionDescription?.let {
                    // 3. We must set this offer as our "Local Description" so the engine locks it in
                    val localObserver = object : org.webrtc.SdpObserver {
                        override fun onCreateSuccess(p0: org.webrtc.SessionDescription?) {}
                        override fun onSetSuccess() {
                            println("APP LOG: Local Offer Set Successfully!")
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {
                            println("APP LOG: Failed to set local offer: $p0")
                        }
                    }

                    peerConnection?.setLocalDescription(localObserver, it)

                    onOfferReady(it.description)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                println("APP LOG: Failed to create offer: $error")
            }
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }
    fun handleRemoteAnswer(sdpString: String) {
        val sessionDescription = org.webrtc.SessionDescription(
            org.webrtc.SessionDescription.Type.ANSWER,
            sdpString
        )

        peerConnection?.setRemoteDescription(object : org.webrtc.SdpObserver {
            override fun onCreateSuccess(p0: org.webrtc.SessionDescription?) {}
            override fun onSetSuccess() {
                println("APP LOG: Remote Answer Set Successfully! Handshake complete.")
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {
                println("APP LOG: Failed to set remote answer: $p0")
            }
        }, sessionDescription)
    }
    fun addRemoteIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
        println("APP LOG: Added Remote ICE Candidate from Car")
    }

    fun sendDataThroughChannel(commandJSON: String){
        if(dataChannel?.state()== DataChannel.State.OPEN){
            val bytes  = commandJSON.toByteArray(Charsets.UTF_8)
            val buffer = ByteBuffer.wrap(bytes)
            dataChannel?.send(DataChannel.Buffer(buffer,false))
        }
    }
}