import java.net.URI

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.scrappy.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.scrappy.app"
        manifestPlaceholders["scrappyLabel"] = "Scrappy"
        minSdk = 26
        targetSdk = 36
        versionCode = 55
        versionName = "4.9.6"
        val aiEndpoint=providers.gradleProperty("scrappyAiEndpoint").orElse("").get()
        buildConfigField("String", "AI_ENDPOINT", "\"" + aiEndpoint.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        val aiPrivacyUrl=providers.gradleProperty("scrappyAiPrivacyUrl").orElse("").get()
        buildConfigField("String", "AI_PRIVACY_URL", "\"" + aiPrivacyUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("SCRAPPY_KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("SCRAPPY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SCRAPPY_KEY_ALIAS") ?: "scrappy-upload"
                keyPassword = System.getenv("SCRAPPY_KEY_PASSWORD") ?: System.getenv("SCRAPPY_KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".preview"
            manifestPlaceholders["scrappyLabel"] = "Scrappy Preview"
        }
        create("cameraTest") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".cameratest"
            manifestPlaceholders["scrappyLabel"] = "Scrappy Camera Test"
            matchingFallbacks += listOf("debug")
        }
        release {
            if (!System.getenv("SCRAPPY_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

// A Play/release build must not silently ship with the headline AI feature disabled.
// Debug/preview builds may omit these values so UI and manual-helper testing still works offline.
val validateScrappyReleaseConfig by tasks.registering {
    group = "verification"
    description = "Fails release packaging when Scrappy production AI/privacy configuration is missing or unsafe."
    doLast {
        fun requireHttps443(name: String, value: String, requiredPath: String? = null) {
            require(value.isNotBlank()) { "$name is required for a Scrappy release build" }
            val uri = URI(value)
            require(uri.scheme == "https" && uri.host != null && uri.userInfo == null && uri.port in listOf(-1, 443)) {
                "$name must be an HTTPS URL on the default TLS port with no embedded credentials"
            }
            require(uri.rawQuery == null && uri.rawFragment == null) { "$name must not contain a query string or fragment" }
            if (requiredPath != null) require(uri.path == requiredPath) { "$name must end at $requiredPath" }
        }
        requireHttps443("scrappyAiEndpoint", providers.gradleProperty("scrappyAiEndpoint").orElse("").get(), "/v1/identify")
        requireHttps443("scrappyAiPrivacyUrl", providers.gradleProperty("scrappyAiPrivacyUrl").orElse("").get())
    }
}

tasks.configureEach {
    // Gate the shared release build path too, so direct packaging/install tasks
    // cannot bypass production configuration validation.
    if (name == "preReleaseBuild" || name == "assembleRelease" || name == "bundleRelease") dependsOn(validateScrappyReleaseConfig)
}

// Include the real socket transport regressions in the standard Android checks.
tasks.matching { it.name in setOf("testDebugUnitTest", "testCameraTestUnitTest") }.configureEach {
    dependsOn(":transport-tests:test")
}
