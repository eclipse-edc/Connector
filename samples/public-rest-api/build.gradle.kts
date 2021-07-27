/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

val rsApi: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":core:spi"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}


