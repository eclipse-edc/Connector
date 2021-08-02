/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":extensions:aws:s3:s3-schema"))
    implementation(project(":extensions:azure:blob:blob-schema"))
    implementation(project(":extensions:in-memory:metadata-memory"))
    implementation(project(":extensions:in-memory:policy-registry-memory"))
}


