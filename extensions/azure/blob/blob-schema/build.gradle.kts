/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val storageBlobVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":core:schema"))

    implementation("com.azure:azure-storage-blob:${storageBlobVersion}")
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("schema.azure.blob") {
            artifactId = "edc.schema.azure.blob"
            from(components["java"])
        }
    }
}