/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
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
        create<MavenPublication>("azure.blob.provision") {
            artifactId = "azure.blob.provision"
            from(components["java"])
        }
    }
}
