/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":extensions:schema"))
    implementation(project(":extensions:metadata:metadata-memory"))
    implementation(project(":extensions:policy:policy-registry-memory"))
}


