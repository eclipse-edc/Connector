/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val eventGridSdkVersion: String by project

dependencies {
    api(project(":spi"))
    implementation(project(":extensions:schema"))
    implementation("com.azure:azure-messaging-eventgrid:${eventGridSdkVersion}")
}


