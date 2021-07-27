/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":core:spi"))
}
publishing {
    publications {
        create<MavenPublication>("metadata-mem") {
            artifactId = "edc.metadata-memory"
            from(components["java"])
        }
    }
}