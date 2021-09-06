/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
}
