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
        create<MavenPublication>("iam.oauth2") {
            artifactId = "iam.oauth2"
            from(components["java"])
        }
    }
}
