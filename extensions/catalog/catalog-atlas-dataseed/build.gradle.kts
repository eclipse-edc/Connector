/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":extensions:catalog:catalog-atlas"))
    implementation("org.apache.atlas:atlas-client-v2:2.1.0")
}
