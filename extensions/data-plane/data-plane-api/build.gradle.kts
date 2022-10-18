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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

val httpMockServer: String by project
val jerseyVersion: String by project
val okHttpVersion: String by project
val restAssured: String by project
val rsApi: String by project
val servletApi: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:common:web-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:data-plane:data-plane-util"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":extensions:common:junit"))
    
    testImplementation("org.glassfish.jersey.media:jersey-media-multipart:${jerseyVersion}")
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.mock-server:mockserver-netty:${httpMockServer}:shaded")
    testImplementation("org.mock-server:mockserver-client-java:${httpMockServer}:shaded")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-api") {
            artifactId = "data-plane-api"
            from(components["java"])
        }
    }
}
