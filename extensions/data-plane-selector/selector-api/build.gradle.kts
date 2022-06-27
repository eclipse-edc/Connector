/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

val mockitoVersion: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

val rsApi: String by project
val okHttpVersion: String by project


dependencies {
    api(project(":spi:core-spi"))
    api(project(":extensions:http"))
    api(project(":extensions:filesystem:configuration-fs"))
    api(project(":extensions:data-plane-selector:selector-spi"))
    implementation(project(":common:util"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation(project(":extensions:api:api-core")) //for the exception mapper


    testImplementation(project(":extensions:data-plane-selector:selector-core")) //for the selector impl
    testImplementation(project(":extensions:data-plane-selector:selector-store"))

    testImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    testImplementation(project(":extensions:http"))

    testImplementation(project(":extensions:junit"))

}


publishing {
    publications {
        create<MavenPublication>("data-plane-selector-api") {
            artifactId = "data-plane-selector-api"
            from(components["java"])
        }
    }
}
