/*
 * Copyright (c) 2022 Florian Rusch
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   Florian Rusch - Initial API and Implementation
 */

val infoModelVersion: String by project
val rsApi: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:api:api-core"))
    implementation(project(":core"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(project(":extensions:http"))
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("contractnegotiation-api") {
            artifactId = "contractnegotiation-api"
            from(components["java"])
        }
    }
}
