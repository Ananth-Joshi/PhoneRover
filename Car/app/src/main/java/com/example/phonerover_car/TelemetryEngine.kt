package com.example.phonerover_car

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class TelemetryEngine (context: Context, private val onTelemetryUpdate : (String) -> Unit){
    private var fusedLocationProviderClient: FusedLocationProviderClient
    private var sensorManager: SensorManager

    private var currentLat = 0.0;
    private var currentLng = 0.0;
    private var currentHeading = 0.0;

    init{
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    public fun startTracking(){
        val rotationVectorSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        val sensorEventListener = object: SensorEventListener{
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int){}

            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                if (event != null && event.sensor.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix,event.values)

                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix,orientationAngles)

                    val azimuthRadians = orientationAngles[0]

                    var azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble())

                    if(azimuthDegrees<0){
                        azimuthDegrees += 360
                    }
                    currentHeading = azimuthDegrees
                }
            }
        }
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            println("APP LOG:Compass Engine Started!")
        } else {
            println("APP LOG: This phone does not have a Rotation Vector sensor!")
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000L).build()

        val locationCallback = object : LocationCallback(){
            override fun onLocationResult(result: LocationResult) {
                if (result.lastLocation != null){
                    currentLat = result.lastLocation!!.latitude
                    currentLng = result.lastLocation!!.longitude

                    // --- THE JSON FACTORY ---
                    val telemetryJson = org.json.JSONObject().apply {
                        put("type", "telemetry")
                        put("lat", currentLat)
                        put("lng", currentLng)
                        put("heading", currentHeading)
                    }

                    // --- SHIP IT! ---
                    onTelemetryUpdate(telemetryJson.toString())
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            android.os.Looper.getMainLooper()
        )
    }

}