/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
}

publishing {
    publications {
        create<MavenPublication>("filesystem.vault") {
            artifactId = "filesystem.vault"
            from(components["java"])
        }
    }
}
