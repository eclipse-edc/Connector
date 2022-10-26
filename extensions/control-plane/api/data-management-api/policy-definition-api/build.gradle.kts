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
val jerseyVersion: String by project
val okHttpVersion: String by project
val rsApi: String by project
val restAssured: String by project
val awaitility: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:control-plane:policy-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":spi:common:policy-model"))
    implementation(project(":extensions:common:api:api-core"))
    implementation(project(":extensions:control-plane:api:data-management-api:data-management-api-configuration"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":extensions:common:transaction:transaction-local"))
    testImplementation(project(":extensions:common:junit"))
    testImplementation(testFixtures(project(":core:common:util")))
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
}

publishing {
    publications {
        create<MavenPublication>("policy-definition-api") {
            artifactId = "policy-definition-api"
            from(components["java"])
        }
    }
}
