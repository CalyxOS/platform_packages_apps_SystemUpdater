/*
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.1" apply false
    // https://android.googlesource.com/platform/external/kotlinc/+/refs/tags/android-14.0.0_r25/build.txt
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("com.google.dagger.hilt.android") version "2.44.2" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0" apply false
}
