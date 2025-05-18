/*
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "org.calyxos.systemupdater"
    compileSdk = 35

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
    /**
     * Dependencies in AOSP
     *
     * We try to keep the dependencies in sync with what AOSP ships as SystemUpdater is meant to be
     * built with the AOSP build system and gradle builds are just for more pleasant development.
     * Using the AOSP versions in gradle builds allows us to spot issues early on.
     */
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("android.jar", "libcore.jar"))))

    // AndroidX
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/core/core-ktx?autodive=0
    implementation("androidx.core:core-ktx") {
        version { strictly("1.16.0-alpha01") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/appcompat/appcompat?autodive=0
    implementation("androidx.appcompat:appcompat") {
        version { strictly("1.7.0") } // 1.8.0-alpha01 in AOSP but not released
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/fragment/fragment-ktx?autodive=0
    implementation("androidx.fragment:fragment-ktx") {
        version { strictly("1.8.6") } // 1.9.0-alpha01 in AOSP but not released
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/activity/activity-ktx?autodive=0
    implementation("androidx.activity:activity-ktx") {
        version { strictly("1.10.0-rc01") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/lifecycle/lifecycle-service?autodive=0
    implementation("androidx.lifecycle:lifecycle-service") {
        version { strictly("2.9.0-alpha08") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/lifecycle/lifecycle-viewmodel-ktx?autodive=0
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx") {
        version { strictly("2.9.0-alpha08") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/preference/preference?autodive=0
    implementation("androidx.preference:preference") {
        version { strictly("1.2.1") } // 1.3.0-alpha01 in AOSP but as not been released
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/navigation/navigation-fragment-ktx?autodive=0
    implementation("androidx.navigation:navigation-fragment-ktx") {
        version { strictly("2.9.0-alpha04") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/navigation/navigation-ui-ktx?autodive=0
    implementation("androidx.navigation:navigation-ui-ktx") {
        version { strictly("2.9.0-alpha04") }
    }
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android15-qpr2-release/current/androidx/m2repository/androidx/work/work-runtime-ktx?autodive=0
    implementation("androidx.work:work-runtime-ktx") {
        version { strictly("2.10.0-rc01") }
    }

    // Google
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/heads/android15-qpr2-release/current/extras/material-design-x/Android.bp#7
    implementation("com.google.android.material:material") {
        version { strictly("1.13.0-alpha08") }
    }
    // https://android.googlesource.com/platform/external/guava/+/refs/heads/android15-qpr2-release/android/pom.xml
    implementation("com.google.guava:guava") {
        version { strictly("32.1.2-android") }
    }
    // https://android.googlesource.com/platform/external/gson/+/refs/heads/master/pom.xml
    implementation("com.google.code.gson:gson") {
        version { strictly("2.10.1") }
    }

    // JetBrains
    // https://android.googlesource.com/platform/external/kotlinx.coroutines/+/refs/heads/android15-qpr2-release/CHANGES.md
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android") {
        version { strictly("1.8.1") }
    }

    // Hilt is an exception due to lack of any specific version source though newer versions requires newer kotlin releases
    // https://android.googlesource.com/platform/external/dagger2/+/refs/heads/android15-qpr2-release
    val hiltVersion = "2.51.1"
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
}
