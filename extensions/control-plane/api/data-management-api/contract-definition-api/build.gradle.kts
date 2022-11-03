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
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":extensions:common:api:api-core"))
    implementation(project(":extensions:control-plane:api:data-management-api:data-management-api-configuration"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.awaitility:awaitility:${awaitility}")
}

publishing {
    publications {
        create<MavenPublication>("contract-definition-api") {
            artifactId = "contract-definition-api"
            from(components["java"])
        }
    }
}
