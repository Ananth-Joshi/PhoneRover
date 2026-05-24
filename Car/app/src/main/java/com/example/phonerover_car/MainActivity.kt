package com.example.phonerover_car

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.IO
import io.socket.client.Socket

class MainActivity : AppCompatActivity() {
    private lateinit var socket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val roomId = "signalling-room"
        socket = IO.socket(BuildConfig.SIGNALING_SERVER_URL)

        socket.on(Socket.EVENT_CONNECT){
            println("connected to signalling server")
            socket.emit("join-room", roomId)
        }
        socket.connect()

        socket.on("offer"){ args: Array<Any> ->
            try{

            }catch (e: Exception){

            }
        }
    }
}