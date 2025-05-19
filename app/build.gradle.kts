/*
 * SPDX-FileCopyrightText: 2022-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt.android.plugin)
    alias(libs.plugins.jlleitschuh.ktlint)
}

android {
    namespace = "org.calyxos.systemupdater"
    compileSdk = 35

    defaultConfig {
        minSdk = 35
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        register("aosp") {
            // Generated from the AOSP testkey:
            // https://android.googlesource.com/platform/build/+/refs/tags/android-11.0.0_r29/target/product/security/testkey.pk8
            keyAlias = "testkey"
            keyPassword = "testkey"
            storeFile = file("testkey.jks")
            storePassword = "testkey"
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("aosp")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        languageVersion = "1.9"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

hilt {
    enableAggregatingTask = false
}

dependencies {
    // AOSP
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("android.jar", "libcore.jar"))))

    // AndroidX
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.work.runtime)

    // Google
    implementation(libs.google.android.material)
    implementation(libs.google.guava)

    // JetBrains
    implementation(libs.jetbrains.kotlin.coroutines)
    implementation(libs.jetbrains.kotlin.serialization)

    // Hilt
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.android.core)
}
