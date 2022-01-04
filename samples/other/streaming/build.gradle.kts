/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

val jettyVersion: String by project
val websocketVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    api(project(":extensions:inline-data-transfer:inline-data-transfer-spi"))
    implementation(project(":extensions:inline-data-transfer:inline-data-transfer-core"))
    implementation(project(":core:base"))
    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation("jakarta.websocket:jakarta.websocket-api:${websocketVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    // extensions needed for integration testing
    testImplementation(project(":core:transfer"))
    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
    testImplementation(testFixtures(project(":launchers:junit")))

}