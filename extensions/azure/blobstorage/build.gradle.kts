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


//this file serves as BOM for blobstorage
dependencies {
    api(project(":spi"))
    api(project(":extensions:azure:blobstorage:blob-data-operator"))
    api(project(":extensions:azure:blobstorage:blob-core"))
    api(project(":extensions:azure:blobstorage:blob-provision"))
}

publishing {
    publications {
        create<MavenPublication>("blobstorage") {
            artifactId = "blobstorage"
            from(components["java"])
        }
    }
}
