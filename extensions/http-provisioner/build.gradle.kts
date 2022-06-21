/*
 *  Copyright (c) 2022 Microsoft Corporation
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

val okHttpVersion: String by project
val jodahFailsafeVersion: String by project
val rsApi: String by project
val restAssured: String by project
val jerseyVersion: String by project


dependencies {
    api(project(":spi:transfer-spi"))
    api(project(":spi:web-spi"))
    implementation(project(":extensions:api:auth-spi"))
    implementation(project(":core:transfer")) // needs the AddProvisionedResourceCommand

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":core:contract"))
    testImplementation(project(":extensions:dataloading"))
    testImplementation(project(":core:defaults"))


    testImplementation(project(":extensions:junit"))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testRuntimeOnly("org.glassfish.jersey.ext:jersey-bean-validation:${jerseyVersion}") //for validation
}


publishing {
    publications {
        create<MavenPublication>("http-provisioner") {
            artifactId = "http-provisioner"
            from(components["java"])
        }
    }
}
