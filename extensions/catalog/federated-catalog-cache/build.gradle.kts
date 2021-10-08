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

    implementation(project(":extensions:catalog:federated-catalog-spi"))
    implementation(project(":common:util"))
    implementation("org.junit.jupiter:junit-jupiter:5.7.0")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    // generates random names
    // fixme: remove before PR submit
    implementation("info.schnatterer.moby-names-generator:moby-names-generator:20.10.0-r0")

    testImplementation(project(":core:bootstrap")) //for the console monitor

    // required for integration test
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(project(":core:protocol:web"))
    testImplementation(project(":extensions:in-memory:fcc-protocol-registry-memory"))
    testImplementation(project(":extensions:in-memory:fcc-query-adapter-registry-memory"))
    testImplementation(project(":extensions:in-memory:node-directory-memory"))
    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
    testImplementation(project(":extensions:in-memory:fcc-store-memory"))
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = false
    }
}
publishing {
    publications {
        create<MavenPublication>("catalog.cache") {
            artifactId = "catalog.cache"
            from(components["java"])
        }
    }
}
