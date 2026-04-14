plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.recall.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.recall.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 56
        versionName = "6.1"
        
        resourceConfigurations += listOf("en")
        vectorDrawables.useSupportLibrary = true
    }

    aaptOptions {
        noCompress("task", "onnx")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += listOf("DebugProbesKt.bin", "META-INF/**.version", "kotlin/**.kotlin_builtins", "kotlin-tooling-metadata.json")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Navigation
    val navVersion = "2.7.6"
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")

    // Recall Dependencies
    implementation("org.ocpsoft.prettytime:prettytime:4.0.6.Final")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.github.rambler-digital-solutions:swipe-layout-android:1.0.17")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")

    // AI Dependencies
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.1")
    implementation("com.google.mediapipe:tasks-genai:0.10.14")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
