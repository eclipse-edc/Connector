/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

val infoModelVersion: String by project
val rsApi: String by project
val jerseyVersion: String by project
val restAssured: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:api:auth-spi"))
    api(project(":spi:core-spi"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}

publishing {
    publications {
        create<MavenPublication>("auth-basic") {
            artifactId = "auth-basic"
            from(components["java"])
        }
    }
}
