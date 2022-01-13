/*
 *  Copyright (c) 2021 Microsoft Corporation
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

plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    api(project(":spi"))
    api(project(":extensions:catalog:federated-catalog-spi"))
    implementation(project(":core:base"))
    implementation(project(":common:util"))
    implementation(project(":extensions:http"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    // required for integration test
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(project(":core:base"))
    testImplementation(project(":data-protocols:ids:ids-spi"))
    testImplementation(project(":extensions:in-memory:fcc-node-directory-memory"))
    testImplementation(project(":extensions:in-memory:fcc-store-memory"))
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = false
    }
}
publishing {
    publications {
        create<MavenPublication>("catalog-cache") {
            artifactId = "catalog-cache"
            from(components["java"])
        }
    }
}