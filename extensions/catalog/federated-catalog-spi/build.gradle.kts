/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
}
publishing {
    publications {
        create<MavenPublication>("catalog.spi") {
            artifactId = "catalog.spi"
            from(components["java"])
        }
    }
}

