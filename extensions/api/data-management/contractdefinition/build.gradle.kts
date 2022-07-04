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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

val awaitility: String by project
val jerseyVersion: String by project
val restAssured: String by project
val rsApi: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:transaction-spi"))
    implementation(project(":extensions:api:api-core"))
    implementation(project(":extensions:api:auth-spi"))
    implementation(project(":extensions:api:data-management:api-configuration"))
    implementation(project(":extensions:dataloading"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":extensions:http"))

    testImplementation(project(":extensions:junit"))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.awaitility:awaitility:${awaitility}")
}

publishing {
    publications {
        create<MavenPublication>("contractdefinition-api") {
            artifactId = "contractdefinition-api"
            from(components["java"])
        }
    }
}
