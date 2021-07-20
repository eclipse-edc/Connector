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
        create<MavenPublication>("proto-loopback") {
            artifactId = "edc.protocol-loopback"
            from(components["java"])
        }
    }
}