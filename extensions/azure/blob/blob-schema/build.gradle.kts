/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val storageBlobVersion: String by project

dependencies {
    api(project(":edc-core:spi"))
    api(project(":edc-core:schema"))

    implementation("com.azure:azure-storage-blob:${storageBlobVersion}")
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("transfer-provision-azure") {
            artifactId = "edc.transfer-provision-azure"
            from(components["java"])
        }
    }
}