/*
 * Copyright (c) 2022 cluetec GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   cluetec GmbH - Initial API and Implementation
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
    api(project(":common:util"))
    api(project(":extensions:http"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":core:"))



    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation("io.rest-assured:rest-assured:4.4.0")
}

publishing {
    publications {
        create<MavenPublication>("control-api") {
            artifactId = "control-api"
            from(components["java"])
        }
    }
}
