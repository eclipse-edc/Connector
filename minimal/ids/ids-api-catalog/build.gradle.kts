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
    api(project(":edc:spi"))
    api(project(":minimal:ids:ids-spi"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")
    
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}

publishing {
    publications {
        create<MavenPublication>("ids-api-catalog") {
            artifactId = "edc.ids-api-catalog"
            from(components["java"])
        }
    }
}