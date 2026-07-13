import java.util.Properties
import java.net.Inet4Address
import java.net.NetworkInterface

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

fun resolveDeviceApiHost(): String {
    val configuredHost = providers.gradleProperty("deviceApiHost").orNull
        ?: System.getenv("RESP_AI_DEVICE_HOST")

    if (!configuredHost.isNullOrBlank()) {
        return configuredHost
    }

    return NetworkInterface.getNetworkInterfaces()
        .toList()
        .asSequence()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .flatMap { network -> network.inetAddresses.toList().asSequence() }
        .filterIsInstance<Inet4Address>()
        .map { it.hostAddress }
        .firstOrNull { host ->
            host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.startsWith("172.")
        }
        ?: "10.0.2.2"
}

fun normalizeBaseUrl(url: String): String {
    return if (url.endsWith("/")) url else "$url/"
}

fun resolveDeviceApiBaseUrl(): String {
    val configuredBaseUrl = providers.gradleProperty("deviceApiBaseUrl").orNull
        ?: System.getenv("RESP_AI_DEVICE_BASE_URL")

    if (!configuredBaseUrl.isNullOrBlank()) {
        return normalizeBaseUrl(configuredBaseUrl)
    }

    return "http://${resolveDeviceApiHost()}:8080/"
}

val deviceApiBaseUrl = resolveDeviceApiBaseUrl()

android {
    namespace = "com.respiratoryai.mobile"
    compileSdk = 36

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.respiratoryai.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "EMULATOR_API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        buildConfigField("String", "DEVICE_API_BASE_URL", "\"$deviceApiBaseUrl\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "SHOW_CONNECTION_SETTINGS", "true")
        }
        create("qa") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            buildConfigField("boolean", "SHOW_CONNECTION_SETTINGS", "true")
            versionNameSuffix = "-qa"
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "SHOW_CONNECTION_SETTINGS", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.documentfile:documentfile:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
