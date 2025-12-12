plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

val openAiKeyProvider = providers
    .gradleProperty("OPENAI_API_KEY")
    .orElse(providers.environmentVariable("OPENAI_API_KEY"))

val enableIrtScoringProvider = providers
    .gradleProperty("enableIrtScoring")
    .map { it.toBoolean() }
    .orElse(false)

android {
    namespace = "com.choicecrafter.students"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.choicecrafter.students"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        if (!openAiKeyProvider.isPresent) {
            require(false) { "Missing OpenAI API key. Provide it via the OPENAI_API_KEY Gradle property or environment variable." }
        }
        val openAiKey = openAiKeyProvider.get()
        val sanitizedKey = openAiKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"$sanitizedKey\"")

        val irtScoringEnabled = enableIrtScoringProvider.get()
        buildConfigField("boolean", "ENABLE_IRT_SCORING", irtScoringEnabled.toString())
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
        viewBinding = true
    }
}

flutter {
    source = "../.."
}

dependencies {
    // AndroidX & UI Libraries
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.annotation:annotation:1.7.1")

    // Firebase (using Bill of Materials
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    // Note: The following debug dependency is often version-specific. Using a recent one.
    debugImplementation("com.google.firebase:firebase-appcheck-debug:17.0.1")

    // Google Play Services
    implementation("com.google.android.gms:play-services-base:18.4.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Utility Libraries
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("org.json:json:20240303")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    // This is likely another JUnit extension, using a common version for it.
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.test:runner:1.7.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}

apply(plugin = "com.google.gms.google-services")

