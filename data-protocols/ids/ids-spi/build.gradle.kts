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
    api(project(":spi"))

    api(project(":core:policy:policy-engine"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}


publishing {
    publications {
        create<MavenPublication>("data-protocols.ids-spi") {
            artifactId = "data-protocols..ids-spi"
            from(components["java"])
        }
    }
}
