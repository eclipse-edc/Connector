/*
 *  Copyright (c) 2020, 2022 Microsoft Corporation
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


val restAssured: String by project
val rsApi: String by project
val awaitility: String by project
val jakartaValidationApi: String by project
val jerseyVersion: String by project


dependencies {

    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:control-plane:control-plane-api-client-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:common:auth-spi"))


    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("jakarta.validation:jakarta.validation-api:${jakartaValidationApi}")
    implementation("org.glassfish.jersey.ext:jersey-bean-validation:${jerseyVersion}") //for validation


    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":extensions:common:junit"))
    testImplementation(project(":extensions:common:auth:auth-tokenbased"))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.awaitility:awaitility:${awaitility}")

}


publishing {
    publications {
        create<MavenPublication>("control-plane-api") {
            artifactId = "control-plane-api"
            from(components["java"])
        }
    }
}
