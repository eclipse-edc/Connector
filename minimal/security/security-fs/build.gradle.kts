/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":edc:spi"))
}

publishing {
    publications {
        create<MavenPublication>("security-fs") {
            artifactId = "edc.security-fs"
            from(components["java"])
        }
    }
}