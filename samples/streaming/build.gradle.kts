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
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":core:protocol:web"))
    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation("jakarta.websocket:jakarta.websocket-api:${websocketVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    // extensions needed for integration testing
    testImplementation(project(":core:transfer"))
    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
    testImplementation(testFixtures(project(":common:util")))

}


//publishing {
//    publications {
//        create<MavenPublication>("samples.streaming") {
//            artifactId = "dataspaceconnector.samples.streaming"
//            from(components["java"])
//        }
//    }
//}
