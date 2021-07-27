/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    id("application")
}

dependencies {
    api(project(":core:bootstrap"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}
publishing {
    publications {
        create<MavenPublication>("runtime") {
            artifactId = "edc.runtime"
            from(components["java"])
        }
    }
}