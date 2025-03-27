plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.darkwhite.opencvfacedetection"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.darkwhite.opencvfacedetection"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation (fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation (project(":openCV"))

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2023.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    //implementation(libs.androidx.runtime.livedata)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation ("androidx.navigation:navigation-compose:2.6.0")

    implementation("androidx.compose.material:material:1.5.1")


    val cameraxVersion = "1.3.0-rc01"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.28.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation( "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.x.x")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.x.x")
    implementation ("androidx.compose.runtime:runtime:1.x.x")
    implementation ("com.google.accompanist:accompanist-permissions:0.31.5-beta")

    implementation ("com.google.mlkit:face-detection:16.1.5")


    implementation ("org.jmrtd:jmrtd:0.7.18")
    implementation ("net.sf.scuba:scuba-sc-android:0.0.20")
    implementation ("com.madgag.spongycastle:prov:1.58.0.0")
    implementation ("edu.ucar:jj2000:5.2")
    implementation ("com.github.mhshams:jnbis:1.1.0")


    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okio:okio:2.10.0")
    implementation("androidx.compose.foundation:foundation:1.4.0")


    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")
    implementation("com.airbnb.android:lottie-compose:6.0.0")


    //tenforflowlite
    implementation ("org.tensorflow:tensorflow-lite:2.9.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.2")


    implementation (fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation (project(":openCV"))

    implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")
}