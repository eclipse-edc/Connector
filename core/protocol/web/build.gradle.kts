/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
val servletApi: String by project
val rsApi: String by project
val jettyVersion: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))

    implementation("org.eclipse.jetty:jetty-webapp:${jettyVersion}") {
        exclude("jetty-xml")
    }

    implementation("org.glassfish.jersey.core:jersey-server:${jerseyVersion}")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet-core:${jerseyVersion}")
    implementation("org.glassfish.jersey.core:jersey-common:${jerseyVersion}")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:${jerseyVersion}")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:${jerseyVersion}")
    implementation("org.glassfish.jersey.inject:jersey-hk2:${jerseyVersion}")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:${jerseyVersion}")

    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation("jakarta.websocket:jakarta.websocket-api:2.0.0")

}

publishing {
    publications {
        create<MavenPublication>("core.protocol-web") {
            artifactId = "core.protocol-web"
            from(components["java"])
        }
    }
}
