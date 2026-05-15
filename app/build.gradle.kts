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
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "app"
    productFlavors {
        create("dartat") {
            dimension = "app"
            applicationId = "com.dkonak.dartat"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
