plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        versionCode = 5
        versionName = "1.3.0"
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
            applicationIdSuffix = ".debug"
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
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
}
