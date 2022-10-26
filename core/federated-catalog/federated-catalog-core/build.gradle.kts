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
val failsafeVersion: String by project
val okHttpVersion: String by project
val awaitility: String by project


dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:federated-catalog:federated-catalog-spi"))

    implementation(project(":core:common:util"))
    implementation(project(":core:common:connector-core"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("dev.failsafe:failsafe:${failsafeVersion}")

    // required for integration test
    testImplementation(project(":extensions:common:junit"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(testFixtures(project(":core:common:util")))
    testImplementation(project(":data-protocols:ids:ids-spi"))
    testImplementation("org.awaitility:awaitility:${awaitility}")

}

publishing {
    publications {
        create<MavenPublication>("federated-catalog-core") {
            artifactId = "federated-catalog-core"
            from(components["java"])
        }
    }
}
