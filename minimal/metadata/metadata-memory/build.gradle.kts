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
        create<MavenPublication>("metadata-mem") {
            artifactId = "edc.metadata-memory"
            from(components["java"])
        }
    }
}