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
        create<MavenPublication>("catalog-cache-store-memory") {
            artifactId = "catalog-cache-store-memory"
            from(components["java"])
        }
    }
}
