/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}
val storageBlobVersion: String by project;

dependencies {
    api(project(":edc:core"))
    api(project(":edc:transfer"))
    api(project(":minimal:transfer:transfer-store-memory"))

    api(project(":extensions:aws:s3:provision"))
    api(project(":extensions:azure:blob:provision"))

    api(project(":minimal:ids:ids-core"))

    testImplementation(project(":minimal:configuration:configuration-fs"))
    testImplementation(project(":extensions:azure:vault"))
    testImplementation("com.azure:azure-storage-blob:${storageBlobVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
    testImplementation(testFixtures(project(":common:util")))


}
