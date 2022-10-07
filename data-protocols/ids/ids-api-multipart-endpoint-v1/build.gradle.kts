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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependency
 *
 */
val infoModelVersion: String by project
val rsApi: String by project
val jerseyVersion: String by project
val okHttpVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:ids:ids-spi"))
    api(project(":data-protocols:ids:ids-core"))
    api(project(":data-protocols:ids:ids-transform-v1"))
    api(project(":extensions:common:http"))

    implementation(project(":data-protocols:ids:ids-api-configuration"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:${jerseyVersion}")

    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.36.0")
    testImplementation("net.javacrumbs.json-unit:json-unit-json-path:2.36.0")
    testImplementation("net.javacrumbs.json-unit:json-unit:2.35.0")

    testImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    testImplementation(project(":extensions:common:junit"))

    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(testFixtures(project(":core:common:util")))

}

publishing {
    publications {
        create<MavenPublication>("ids-api-multipart-endpoint-v1") {
            artifactId = "ids-api-multipart-endpoint-v1"
            from(components["java"])
        }
    }
}
