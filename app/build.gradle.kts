plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.kanjiwidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kanjiwidget"
        minSdk = 24
        targetSdk = 34
        versionCode = 15
        versionName = "1.7.1"
        manifestPlaceholders["appLabel"] = "Kanji Widget"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("release.storeFile")?.trim()
            val storePasswordValue = localProperties.getProperty("release.storePassword")?.trim()
            val keyAliasValue = localProperties.getProperty("release.keyAlias")?.trim()
            val keyPasswordValue = localProperties.getProperty("release.keyPassword")?.trim()

            if (!storeFilePath.isNullOrEmpty()
                && !storePasswordValue.isNullOrEmpty()
                && !keyAliasValue.isNullOrEmpty()
                && !keyPasswordValue.isNullOrEmpty()
            ) {
                storeFile = file(storeFilePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "Kanji Widget Debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.mlkit:translate:17.0.3")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
}
