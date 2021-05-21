/*
* Copyright (c) Microsoft Corporation.
* All rights reserved.
*/

val jettyVersion: String by project
val websocketVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    implementation(project(":extensions:protocol:web"))
    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation("jakarta.websocket:jakarta.websocket-api:${websocketVersion}")

    // extensions needed for integration testing
    testImplementation(project(":distributions:junit"))
    testImplementation(project(":extensions:protocol:protocol-loopback"))
    testImplementation(project(":extensions:transfer:transfer-core"))
    testImplementation(project(":extensions:transfer:transfer-store-memory"))

}


