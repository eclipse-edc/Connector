/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}
val storageBlobVersion: String by project;

dependencies {
    api(project(":core"))
    api(project(":extensions:transfer:transfer-core"))
    api(project(":extensions:transfer:transfer-store-memory"))

    api(project(":extensions:transfer:transfer-provision-aws"))
    api(project(":extensions:transfer:transfer-provision-azure"))

    api(project(":extensions:ids:ids-core"))

    testImplementation(project(":extensions:security:security-fs"))
    testImplementation(project(":distributions:junit"))
    implementation("com.azure:azure-storage-blob:${storageBlobVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")


}
