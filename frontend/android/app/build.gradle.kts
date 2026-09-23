plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.atu.cafeteria"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "com.atu.cafeteria"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    // Production signing is supplied through environment variables or Gradle
    // properties and is never committed to source control. Local/CI builds may
    // continue using debug signing unless production signing is explicitly
    // required with ATU_REQUIRE_PROD_SIGNING=true.
    val requireProductionSigning =
        System.getenv("ATU_REQUIRE_PROD_SIGNING")?.toBoolean() == true

    val keystorePath = System.getenv("ATU_KEYSTORE_PATH")
    val keystorePassword = System.getenv("ATU_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("ATU_KEY_ALIAS")
    val keyPassword = System.getenv("ATU_KEY_PASSWORD")

    signingConfigs {
        if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
            create("production") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        } else if (requireProductionSigning) {
            throw GradleException(
                "Production signing is required but ATU_KEYSTORE_PATH, " +
                    "ATU_KEYSTORE_PASSWORD, ATU_KEY_ALIAS and ATU_KEY_PASSWORD " +
                    "were not supplied."
            )
        }
    }

    buildTypes {
        release {
            if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() &&
                !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("production")
            } else {
                // Local/defense builds remain buildable. Production CI sets
                // ATU_REQUIRE_PROD_SIGNING=true and therefore cannot use this fallback.
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
}

flutter {
    source = "../.."
}
