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

    testFixturesApi(platform("com.amazonaws:aws-java-sdk-bom:1.11.1018"))
    testFixturesApi("com.amazonaws:aws-java-sdk-s3")
    testFixturesApi("com.azure:azure-storage-blob:${storageBlobVersion}")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

publishing {
    publications {
        create<MavenPublication>("common.util") {
            artifactId = "edc.common.util"
            from(components["java"])
        }
    }
}