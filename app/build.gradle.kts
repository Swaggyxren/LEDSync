plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiannn.ledtile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xiannn.ledtile"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val storeFileProp = (findProperty("LEDTILE_STORE_FILE") as String?)
                ?: System.getenv("LEDTILE_STORE_FILE")
                ?: "../release.keystore"
            val storePasswordProp = (findProperty("LEDTILE_STORE_PASSWORD") as String?)
                ?: System.getenv("LEDTILE_STORE_PASSWORD")
                ?: "ledtile123"
            val keyAliasProp = (findProperty("LEDTILE_KEY_ALIAS") as String?)
                ?: System.getenv("LEDTILE_KEY_ALIAS")
                ?: "ledtile"
            val keyPasswordProp = (findProperty("LEDTILE_KEY_PASSWORD") as String?)
                ?: System.getenv("LEDTILE_KEY_PASSWORD")
                ?: "ledtile123"
            val ks = file(storeFileProp)
            if (ks.exists()) {
                storeFile = ks
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
}
