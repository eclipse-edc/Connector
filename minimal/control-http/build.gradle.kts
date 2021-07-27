/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

val rsApi: String by project

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":core:spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}


publishing {
    publications {
        create<MavenPublication>("control-http") {
            artifactId = "edc.control-http"
            from(components["java"])
        }
    }
}