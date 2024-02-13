/*
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "org.calyxos.systemupdater"
    compileSdk = 34

    defaultConfig {
        minSdk = 34
        targetSdk = 34
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
        languageVersion = "1.7"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

kapt {
    correctErrorTypes = true
}

hilt {
    enableAggregatingTask = false
}

dependencies {
    /**
     * Dependencies in AOSP
     *
     * We try to keep the dependencies in sync with what AOSP ships as SystemUpdater is meant to be
     * built with the AOSP build system and gradle builds are just for more pleasant development.
     * Using the AOSP versions in gradle builds allows us to spot issues early on.
     */
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("android.jar", "libcore.jar"))))

    // AndroidX
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#2741
    implementation("androidx.core:core-ktx") {
        version { strictly("1.12.0-alpha05") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#243
    implementation("androidx.appcompat:appcompat") {
        version { strictly("1.7.0-alpha03") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#3245
    implementation("androidx.fragment:fragment-ktx") {
        version { strictly("1.6.0-alpha08") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#61
    implementation("androidx.activity:activity-ktx") {
        version { strictly("1.7.0-beta01") } // 1.7.0-alpha05 in AOSP but was never released
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#4012
    implementation("androidx.lifecycle:lifecycle-service") {
        version { strictly("2.6.0-alpha04") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#4084
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx") {
        version { strictly("2.6.0-alpha04") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#5023
    implementation("androidx.preference:preference") {
        version { strictly("1.2.0-alpha01") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#4608
    implementation("androidx.navigation:navigation-fragment-ktx") {
        version { strictly("2.7.0-beta01") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#4750
    implementation("androidx.navigation:navigation-ui-ktx") {
        version { strictly("2.7.0-beta01") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/androidx/Android.bp#7029
    implementation("androidx.work:work-runtime-ktx") {
        version { strictly("2.9.0-alpha01") }
    }

    // Google
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r25/current/extras/material-design-x/Android.bp#15
    implementation("com.google.android.material:material") {
        version { strictly("1.7.0-alpha03") }
    }
    // https://android.googlesource.com/platform/external/guava/+/refs/tags/android-14.0.0_r25/android/pom.xml
    implementation("com.google.guava:guava") {
        version { strictly("31.1-android") }
    }
    // https://android.googlesource.com/platform/external/gson/+/refs/heads/master/pom.xml
    implementation("com.google.code.gson:gson") {
        version { strictly("2.10.1") }
    }

    // JetBrains
    // https://android.googlesource.com/platform/external/kotlinx.coroutines/+/refs/tags/android-14.0.0_r25/CHANGES.md
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android") {
        version { strictly("1.7.2") }
    }

    // Hilt is an exception due to lack of any specific version source
    // https://android.googlesource.com/platform/external/dagger2/+/refs/tags/android-13.0.0_r3
    val hiltVersion = "2.44.2"
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
}
