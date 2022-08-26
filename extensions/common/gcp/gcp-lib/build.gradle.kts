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

val googleCloudLibariesVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:control-plane-spi"))

    implementation(platform("com.google.cloud:libraries-bom:${googleCloudLibariesVersion}"))
    implementation("com.google.cloud:google-cloud-storage")
    implementation("com.google.cloud:google-cloud-iamcredentials")
    implementation("com.google.cloud:google-iam-admin")
}
publishing {
    publications {
        create<MavenPublication>("gcp-lib") {
            artifactId = "gcp-lib"
            from(components["java"])
        }
    }
}
