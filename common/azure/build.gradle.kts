/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val storageBlobVersion: String by project;

dependencies {
    api(project(":spi"))
    api(project(":common:util"))
    api("com.azure:azure-storage-blob:${storageBlobVersion}")


    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

publishing {
    publications {
        create<MavenPublication>("common.azure") {
            artifactId = "edc.common.azure"
            from(components["java"])
        }
    }
}