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
    compileSdk = 33

    defaultConfig {
        minSdk = 33
        targetSdk = 33
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
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#1727
    implementation("androidx.core:core-ktx") {
        version { strictly("1.9.0") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#195
    implementation("androidx.appcompat:appcompat") {
        version { strictly("1.4.0-alpha03") } // should be 1.4.0-alpha04, but that is not available via maven
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#2159
    implementation("androidx.fragment:fragment-ktx") {
        version { strictly("1.5.1") } // should be 1.4.0-alpha09, but that has ViewModel issues
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#57
    implementation("androidx.activity:activity-ktx") {
        version { strictly("1.4.0-alpha02") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#2722
    implementation("androidx.lifecycle:lifecycle-service") {
        version { strictly("2.4.0") } // 2.4.0-alpha04 in AOSP but was never released
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#2754
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx") {
        version { strictly("2.4.0") } // 2.4.0-alpha04 in AOSP but was never released
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#3597
    implementation("androidx.preference:preference") {
        version { strictly("1.2.0-alpha01") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#3220
    implementation("androidx.navigation:navigation-fragment-ktx") {
        version { strictly("2.4.0-alpha09") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#3358
    implementation("androidx.navigation:navigation-ui-ktx") {
        version { strictly("2.4.0-alpha09") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/androidx/Android.bp#4892
    implementation("androidx.work:work-runtime-ktx") {
        version { strictly("2.7.0-beta01") }
    }

    // Google
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-13.0.0_r3/current/extras/material-design-x/Android.bp#6
    implementation("com.google.android.material:material") {
        version { strictly("1.6.0") } // 1.6.0-alpha0301 in AOSP but that has minSDK issues
    }
    // https://android.googlesource.com/platform/external/guava/+/refs/tags/android-13.0.0_r3/android/pom.xml
    implementation("com.google.guava:guava") {
        version { strictly("31.0.1-android") }
    }
    // https://android.googlesource.com/platform/external/gson/+/refs/heads/master/pom.xml
    implementation("com.google.code.gson:gson") {
        version { strictly("2.10") }
    }

    // JetBrains
    // https://android.googlesource.com/platform/external/kotlinx.coroutines/+/refs/tags/android-13.0.0_r3/CHANGES.md
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android") {
        version { strictly("1.5.2") }
    }

    // Hilt is an exception due to lack of any specific version source
    // https://android.googlesource.com/platform/external/dagger2/+/refs/tags/android-13.0.0_r3
    val hiltVersion = "2.44.2"
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
}
