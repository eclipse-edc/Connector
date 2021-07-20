/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val cosmosSdkVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":common:util"))

    implementation("com.azure:azure-cosmos:${cosmosSdkVersion}")
}


publishing {
    publications {
        create<MavenPublication>("transfer-store-cosmos") {
            artifactId = "edc.transfer-store-cosmos"
            from(components["java"])
        }
    }
}