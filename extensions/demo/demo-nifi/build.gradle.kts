/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":edc-core:spi"))
    implementation(project(":extensions:aws:s3:s3-schema"))
    implementation(project(":extensions:azure:blob:blob-schema"))
    implementation(project(":minimal:metadata:metadata-memory"))
    implementation(project(":minimal:policy:policy-registry-memory"))
}


