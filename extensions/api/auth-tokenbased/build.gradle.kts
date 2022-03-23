/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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


val infoModelVersion: String by project
val rsApi: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
}

dependencies {

    api(project(":extensions:api:auth-spi"))
    api(project(":extensions:api:api-core"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}

publishing {
    publications {
        create<MavenPublication>("auth-tokenbased") {
            artifactId = "auth-tokenbased"
            from(components["java"])
        }
    }
}