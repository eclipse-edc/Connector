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
val okHttpVersion: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:api:api-core"))
    implementation(project(":extensions:api:data-management:api-configuration"))


    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(project(":extensions:in-memory:contractdefinition-store-memory"))
    testImplementation(project(":extensions:http"))
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("contractdefinition-api") {
            artifactId = "contractdefinition-api"
            from(components["java"])
        }
    }
}
