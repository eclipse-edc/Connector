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
    api(project(":extensions:azure:blob:api"))
    api(project(":extensions:azure:blob:blob-schema"))

    implementation("com.azure:azure-storage-blob:${storageBlobVersion}")

    testImplementation(testFixtures(project(":common:util")))

}

publishing {
    publications {
        create<MavenPublication>("provision.azure.blob") {
            artifactId = "dataspaceconnector.provision.azure.blob"
            from(components["java"])
        }
    }
}
