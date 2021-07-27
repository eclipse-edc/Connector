/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":core:spi"))
    implementation(project(":extensions:catalog-atlas"))
}
