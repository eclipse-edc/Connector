/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
}

val storageBlobVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":extensions:inline-data-transfer:inline-data-transfer-spi"))

    implementation(project(":extensions:azure:blobstorage:blob-core"))
}

publishing {
    publications {
        create<MavenPublication>("blob-operator") {
            artifactId = "blob-operator"
            from(components["java"])
        }
    }
}
