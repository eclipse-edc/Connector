/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

val slf4jVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    api("org.slf4j:slf4j-api:${slf4jVersion}")
}

