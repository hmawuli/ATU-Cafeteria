plugins {
    id("com.android.application")
    id("kotlin-android")
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

    // Production signing is supplied only through environment variables.
    // The keystore and credentials are never committed to source control.
    val requireProductionSigning =
        System.getenv("ATU_REQUIRE_PROD_SIGNING")?.toBoolean() == true

    val keystorePath = System.getenv("ATU_KEYSTORE_PATH")
    val keystorePassword = System.getenv("ATU_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("ATU_KEY_ALIAS")
    val keyPassword = System.getenv("ATU_KEY_PASSWORD")

    val hasProductionSigning =
        !keystorePath.isNullOrBlank() &&
        !keystorePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    signingConfigs {
        if (hasProductionSigning) {
            create("production") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        } else if (requireProductionSigning) {
            throw GradleException(
                "Production signing is required but the production keystore " +
                    "configuration was not supplied."
            )
        }
    }

    buildTypes {
        release {
            // Local release builds remain possible for development. Any
            // production release job explicitly requires the protected signing key.
            if (hasProductionSigning) {
                signingConfig = signingConfigs.getByName("production")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
}

flutter {
    source = "../.."
}
