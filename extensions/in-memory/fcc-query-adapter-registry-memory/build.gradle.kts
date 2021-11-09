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
        create<MavenPublication>("in-memory.catalog.cache.query-adapter-registry") {
            artifactId = "in-memory.catalog.cache.query-adapter-registry"
            from(components["java"])
        }
    }
}
