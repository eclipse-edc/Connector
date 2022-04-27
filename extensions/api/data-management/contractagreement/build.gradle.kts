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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

val rsApi: String by project
val restAssured: String by project
val jerseyVersion: String by project


plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:api:api-core"))
    implementation(project(":extensions:api:data-management:api-configuration"))
    implementation(project(":extensions:transaction:transaction-spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":extensions:in-memory:negotiation-store-memory"))
    testImplementation(project(":extensions:http"))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testRuntimeOnly("org.glassfish.jersey.ext:jersey-bean-validation:${jerseyVersion}") //for validation
}

publishing {
    publications {
        create<MavenPublication>("contractagreement-api") {
            artifactId = "contractagreement-api"
            from(components["java"])
        }
    }
}
