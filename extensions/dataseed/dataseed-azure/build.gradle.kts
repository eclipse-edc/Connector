/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val storageBlobVersion: String by project

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))

    implementation(project(":extensions:transfer:transfer-provision-azure"))
}
