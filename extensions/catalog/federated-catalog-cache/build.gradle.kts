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
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

val rsApi: String by project
val jodahFailsafeVersion: String by project
val okHttpVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":extensions:catalog:federated-catalog-spi"))

    implementation(project(":common:util"))
    implementation(project(":core:base"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")

    // required for integration test
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(project(":extensions:http"))
    testImplementation(project(":data-protocols:ids:ids-spi"))
    testImplementation(project(":extensions:in-memory:fcc-node-directory-memory"))
    testImplementation(project(":extensions:in-memory:fcc-store-memory"))
}

publishing {
    publications {
        create<MavenPublication>("catalog-cache") {
            artifactId = "catalog-cache"
            from(components["java"])
        }
    }
}
