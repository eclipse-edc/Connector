/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */


val infoModelVersion: String by project
val rsApi: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":core:protocol:web"))
    testImplementation(project(":data-protocols:ids"))

    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation("io.rest-assured:rest-assured:4.4.0")

    testImplementation(project(":extensions:in-memory:negotiation-store-memory"))

    implementation(project(":common:util"))
}

publishing {
    publications {
        create<MavenPublication>("observability-api") {
            artifactId = "observability-api"
            from(components["java"])
        }
    }
}
