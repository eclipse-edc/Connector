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
    api(project(":data-protocols:ids:ids-spi"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}

publishing {
    publications {
        create<MavenPublication>("data-protocols.ids.api-transfer") {
            artifactId = "data-protocols.ids-api-transfer"
            from(components["java"])
        }
    }
}
