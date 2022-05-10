/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

val jakartaValidationApi: String by project
val jerseyVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:web-spi"))
    api(project(":spi:transport-spi"))

    implementation(project(":common:util"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("jakarta.validation:jakarta.validation-api:${jakartaValidationApi}")

    testImplementation("org.glassfish.jersey.core:jersey-common:${jerseyVersion}")
    testImplementation("org.glassfish.jersey.core:jersey-server:${jerseyVersion}")

    testImplementation(testFixtures(project(":launchers:junit")))
    testRuntimeOnly("org.glassfish.jersey.ext:jersey-bean-validation:${jerseyVersion}") //for validation
}

publishing {
    publications {
        create<MavenPublication>("api-core") {
            artifactId = "api-core"
            from(components["java"])
        }
    }
}
