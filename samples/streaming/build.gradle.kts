/*
* Copyright (c) Microsoft Corporation.
* All rights reserved.
*/

val jettyVersion: String by project
val websocketVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":edc:spi"))
    implementation(project(":common:util"))
    implementation(project(":edc:protocol:web"))
    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation("jakarta.websocket:jakarta.websocket-api:${websocketVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    // extensions needed for integration testing
    testImplementation(project(":edc:transfer"))
    testImplementation(project(":minimal:transfer:transfer-store-memory"))
    testImplementation(testFixtures(project(":common:util")))

}


publishing {
    publications {
        create<MavenPublication>("transfer-demo-protocols") {
            artifactId = "edc.transfer-demo-protocols"
            from(components["java"])
        }
    }
}