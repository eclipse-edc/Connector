/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

val okHttpVersion: String by project
val jodahFailsafeVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":spi:protocol-spi"))
    implementation(project(":extensions:data-plane:data-plane-spi"))
    implementation(project(":extensions:data-plane:data-plane-framework"))
    implementation(project(":extensions:data-plane:data-plane-http"))
    implementation(project(":extensions:data-plane:data-plane-api"))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-server") {
            artifactId = "data-plane-server"
            from(components["java"])
        }
    }
}
