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
        create<MavenPublication>("schema") {
            artifactId = "dataspaceconnector.schema"
            from(components["java"])
        }
    }
}
