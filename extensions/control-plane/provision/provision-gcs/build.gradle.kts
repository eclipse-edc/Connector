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

dependencies {
    api(project(":spi:common:core-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:gcp:gcp-core"))

    implementation(libs.failsafe.core)
    implementation(libs.googlecloud.storage)
    implementation(libs.googlecloud.iam.admin)
    implementation(libs.googlecloud.iam.credentials)
}

publishing {
    publications {
        create<MavenPublication>("provision-gcs") {
            artifactId = "provision-gcs"
            from(components["java"])
        }
    }
}
