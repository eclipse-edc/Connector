/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

plugins {
    `java-library`
}
val failsafeVersion: String by project
val googleCloudLibariesVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:configuration:filesystem-configuration"))

    implementation(project(":extensions:common:gcp:gcp-lib"))

    api("dev.failsafe:failsafe:${failsafeVersion}")
    implementation(platform("com.google.cloud:libraries-bom:${googleCloudLibariesVersion}"))
    implementation("com.google.cloud:google-cloud-storage")
    implementation("com.google.cloud:google-iam-admin")


}

publishing {
    publications {
        create<MavenPublication>("gcs-provision") {
            artifactId = "gcs-provision"
            from(components["java"])
        }
    }
}
