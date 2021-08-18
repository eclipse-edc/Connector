/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:in-memory:metadata-memory"))
    implementation(project(":core:protocol:web"))

    implementation(project(":samples:gaiax-hackathon-1:identity:catalog-service"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}
