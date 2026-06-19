import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.controller"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        buildConfig = true
    }


    defaultConfig {
        applicationId = "com.example.controller"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //Expose signalling server URL to code
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }
        val serverUrl = properties.getProperty("SIGNALING_SERVER_URL") ?: "\"\""
        buildConfigField("String", "SIGNALING_SERVER_URL", serverUrl)
        buildConfigField("String", "TURN_URI", properties.getProperty("TURN_SERVER_URI") ?: "\"turn:0.0.0.0:3478\"")
        buildConfigField("String", "TURN_USER", properties.getProperty("TURN_USERNAME") ?: "\"user\"")
        buildConfigField("String", "TURN_PASS", properties.getProperty("TURN_PASSWORD") ?: "\"pass\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("io.socket:socket.io-client:2.0.0") {
        exclude(group = "org.json", module = "json")
    }
    implementation(libs.virtual.joystick)
    implementation(libs.webrtc)
    implementation(libs.maplibre.android)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)


}