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
        create<MavenPublication>("core.schema") {
            artifactId = "core.schema"
            from(components["java"])
        }
    }
}
