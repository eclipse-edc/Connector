/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi"))
}

publishing {
    publications {
        create<MavenPublication>("configuration-fs") {
            artifactId = "edc.configuration-fs"
            from(components["java"])
        }
    }
}