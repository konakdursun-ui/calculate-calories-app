import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.dkonak.dartat"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.dkonak.dartat"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "app"
    productFlavors {
        create("dartat") {
            dimension = "app"
            applicationId = "com.dkonak.dartat"
            versionCode = 7
            versionName = "1.0.6"
        }
        create("blockblast") {
            dimension = "app"
            applicationId = "com.dkonak.blockblast"
        }
        create("calculatecalories") {
            dimension = "app"
            applicationId = "com.dkonak.calculatecalories"
            val calorieAiEndpoint = providers.gradleProperty("CALORIE_AI_ENDPOINT")
                .orElse("http://10.0.2.2:8787/analyze-food")
                .get()
            buildConfigField("String", "CALORIE_AI_ENDPOINT", "\"$calorieAiEndpoint\"")
        }
        create("aboneliktakibi") {
            dimension = "app"
            applicationId = "com.dkonak.aboneliktakibi"
            versionCode = 18
            versionName = "1.0.17"
        }
        create("galeritakip") {
            dimension = "app"
            applicationId = "com.dkonak.galeritakip"
            versionCode = 6
            versionName = "1.0.5"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
