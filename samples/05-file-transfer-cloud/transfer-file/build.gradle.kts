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

val rsApi: String by project

dependencies {
    api(project(":spi"))
    implementation(project(":extensions:aws:s3:s3-data-operator"))
    implementation(project(":extensions:azure:blobstorage:blob-core"))
    implementation(project(":extensions:azure:blobstorage:blob-data-operator"))
    implementation(project(":extensions:dataloading"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}