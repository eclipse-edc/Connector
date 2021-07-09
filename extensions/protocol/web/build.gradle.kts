/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
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
    implementation("org.glassfish.jersey.inject:jersey-hk2:${jerseyVersion}")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:${jerseyVersion}")

    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation("jakarta.websocket:jakarta.websocket-api:2.0.0")

}

publishing {
    publications {
        create<MavenPublication>("web") {
            artifactId = "edc.protocol-web"
            from(components["java"])
        }
    }
}