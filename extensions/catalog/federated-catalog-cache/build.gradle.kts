/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    implementation(project(":extensions:catalog:federated-catalog-spi"))
    implementation(project(":common:util"))
    implementation("org.junit.jupiter:junit-jupiter:5.7.0")

    testImplementation(project(":core:bootstrap")) //for the console monitor

}
tasks.withType<Test> {
    testLogging {
        showStandardStreams = false
    }
}
