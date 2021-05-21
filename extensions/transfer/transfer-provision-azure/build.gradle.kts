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
    api(project(":extensions:schema"))

    implementation("com.azure:azure-storage-blob:${storageBlobVersion}")

}

