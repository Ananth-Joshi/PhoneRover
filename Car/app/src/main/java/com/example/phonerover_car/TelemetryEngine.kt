package com.example.phonerover_car

import android.content.Context
import android.hardware.SensorManager
import com.google.android.gms.location.FusedLocationProviderClient

class TelemetryEngine {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager

    private var currentLat = 0.0f;
    private var currentLng = 0.0f;
    private var currentHeading = 0.0f;

    public fun startTracking(context: Context,){

    }

}