/*
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.10.0" apply false
    // https://android.googlesource.com/platform/external/kotlinc/+/refs/heads/android15-qpr2-release/build.txt
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0" apply false
}
