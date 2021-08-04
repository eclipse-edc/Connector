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
        create<MavenPublication>("in-mem.metadata") {
            artifactId = "in-mem.metadata"
            from(components["java"])
        }
    }
}
