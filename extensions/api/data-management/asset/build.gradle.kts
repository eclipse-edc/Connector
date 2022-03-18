/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

val infoModelVersion: String by project
val rsApi: String by project
val jerseyVersion: String by project
val okHttpVersion: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(project(":extensions:api:api-core"))
    implementation(project(":extensions:api:auth-spi"))
    implementation(project(":extensions:api:data-management:api-configuration"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    testImplementation(testFixtures(project(":common:util")))
    testImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    testImplementation(project(":extensions:http"))
}

publishing {
    publications {
        create<MavenPublication>("asset-api") {
            artifactId = "asset-api"
            from(components["java"])
        }
    }
}
