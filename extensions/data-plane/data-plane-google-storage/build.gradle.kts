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
 */plugins {
    `java-library`
}

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:gcp:gcp-core"))
    implementation(project(":core:data-plane:data-plane-util"))

    implementation(libs.googlecloud.storage)

    testImplementation(project(":core:data-plane:data-plane-core"))
    testImplementation(project(":core:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-google-storage") {
            artifactId = "data-plane-google-storage"
            from(components["java"])
        }
    }
}
