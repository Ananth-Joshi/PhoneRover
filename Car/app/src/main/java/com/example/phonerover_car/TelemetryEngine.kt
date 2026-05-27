package com.example.phonerover_car

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class TelemetryEngine (context: Context){
    private var fusedLocationProviderClient: FusedLocationProviderClient
    private var sensorManager: SensorManager

    private var currentLat = 0.0;
    private var currentLng = 0.0;
    private var currentHeading = 0.0;

    init{
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }

    public fun startTracking(){
        val rotationVectorSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        val sensorEventListener = object: SensorEventListener{
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int){}

            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                
            }
        }
    }

}