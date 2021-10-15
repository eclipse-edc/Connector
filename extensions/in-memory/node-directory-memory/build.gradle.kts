/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    api(project(":extensions:catalog:federated-catalog-spi"))
    implementation(project(":common:util"))
}
publishing {
    publications {
        create<MavenPublication>("in-memory.catalog.node-directory") {
            artifactId = "in-memory.catalog.node-directory"
            from(components["java"])
        }
    }
}
