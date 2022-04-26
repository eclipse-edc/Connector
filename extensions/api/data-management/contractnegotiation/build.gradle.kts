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
 *   ZF Friedrichshafen AG - Initial API and Implementation
 */

val restAssured: String by project
val rsApi: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(project(":spi:contract-spi"))
    implementation(project(":spi:transfer-spi"))
    implementation(project(":core:contract"))
    implementation(project(":extensions:api:api-core"))
    implementation(project(":extensions:api:data-management:api-configuration"))
    implementation(project(":extensions:transaction:transaction-spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(project(":common:util"))
    testImplementation(project(":extensions:http"))
    testImplementation(project(":extensions:in-memory:assetindex-memory"))
    testImplementation(project(":extensions:in-memory:contractdefinition-store-memory"))
    testImplementation(project(":extensions:in-memory:negotiation-store-memory"))
    testImplementation(project(":extensions:in-memory:policy-store-memory"))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testRuntimeOnly("org.glassfish.jersey.ext:jersey-bean-validation:${jerseyVersion}") //for validation
}

publishing {
    publications {
        create<MavenPublication>("contractnegotiation-api") {
            artifactId = "contractnegotiation-api"
            from(components["java"])
        }
    }
}
