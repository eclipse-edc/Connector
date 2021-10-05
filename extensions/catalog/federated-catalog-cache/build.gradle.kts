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
    implementation("org.junit.jupiter:junit-jupiter:5.7.0")


}
