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
val jerseyVersion: String by project
val restAssured: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:transfer-spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:api:api-core"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation(project(":extensions:api:data-management:api-configuration"))
    implementation(project(":extensions:transaction:transaction-spi"))

    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
    testImplementation(project(":core:transfer"))
    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":extensions:http"))
    testImplementation(project(":extensions:in-memory:assetindex-memory"))
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
}

publishing {
    publications {
        create<MavenPublication>("transferprocess-api") {
            artifactId = "transferprocess-api"
            from(components["java"])
        }
    }
}
