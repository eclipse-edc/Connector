/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

val okHttpVersion: String by project
val failsafeVersion: String by project
val gscVersion: String by project
val googleCloudStorageVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:gcp:gcp-core"))

    implementation("com.google.cloud:google-cloud-storage:${googleCloudStorageVersion}")

    implementation("dev.failsafe:failsafe:${failsafeVersion}")

    testImplementation(project(":core:data-plane:data-plane-core"))
    testImplementation(project(":extensions:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-gcs") {
            artifactId = "data-plane-gcs"
            from(components["java"])
        }
    }
}
