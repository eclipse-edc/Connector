/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

val infoModelVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":edc-core:spi"))

    api(project(":edc-core:policy:policy-engine"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}


publishing {
    publications {
        create<MavenPublication>("ids-spi") {
            artifactId = "edc.ids-spi"
            from(components["java"])
        }
    }
}