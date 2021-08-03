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
        create<MavenPublication>("vault.fs") {
            artifactId = "dataspaceconnector.vault.fs"
            from(components["java"])
        }
    }
}
