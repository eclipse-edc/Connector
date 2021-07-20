/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val jwtVersion: String by project

dependencies {
    api(project(":spi"))
    implementation("com.auth0:java-jwt:${jwtVersion}")
}

publishing {
    publications {
        create<MavenPublication>("oauth2") {
            artifactId = "edc.iam.oauth2"
            from(components["java"])
        }
    }
}