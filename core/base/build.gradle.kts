/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

val jupiterVersion: String by project
val slf4jVersion: String by project
val infoModelVersion: String by project
val servletApi: String by project
val rsApi: String by project
val jettyVersion: String by project
val jerseyVersion: String by project
val okHttpVersion: String by project
val jodahFailsafeVersion: String by project


dependencies {
    api(project(":spi"))
    api("org.slf4j:slf4j-api:${slf4jVersion}")

    implementation("org.glassfish.jersey.core:jersey-server:${jerseyVersion}")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet-core:${jerseyVersion}")
    implementation("org.glassfish.jersey.core:jersey-common:${jerseyVersion}")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:${jerseyVersion}")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:${jerseyVersion}")
    implementation("org.glassfish.jersey.inject:jersey-hk2:${jerseyVersion}")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:${jerseyVersion}")

    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation("jakarta.websocket:jakarta.websocket-api:2.0.0")

    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    api("net.jodah:failsafe:${jodahFailsafeVersion}")


    testImplementation("org.awaitility:awaitility:4.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
}

publishing {
    publications {
        create<MavenPublication>("core-base") {
            artifactId = "core-base"
            from(components["java"])
        }
    }
}
