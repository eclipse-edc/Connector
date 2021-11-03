/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */
val infoModelVersion: String by project
val rsApi: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    api(project(":data-protocols:ids:ids-spi"))
    api(project(":data-protocols:ids:ids-core"))
    api(project(":data-protocols:ids:ids-transform-v1"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:${jerseyVersion}")
}

publishing {
    publications {
        create<MavenPublication>("ids.api-multipart-endpoint-v1") {
            artifactId = "data-protocols.ids-api-multipart-endpoint-v1"
            from(components["java"])
        }
    }
}
