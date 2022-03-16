/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

val azureIdentityVersion: String by project
val azureResourceManagerVersion: String by project
val awaitility: String by project
val azureResourceManagerDataFactory: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":extensions:azure:data-plane:common"))
    implementation(project(":common:util"))
    implementation("com.azure:azure-identity:${azureIdentityVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-datafactory:${azureResourceManagerDataFactory}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-storage:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-keyvault:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-authorization:${azureResourceManagerVersion}")

    testImplementation(project(":extensions:filesystem:configuration-fs"))
    testImplementation(project(":extensions:data-plane:data-plane-framework"))
    testImplementation(project(":extensions:azure:resource-manager"))
    testImplementation(testFixtures(project(":extensions:azure:azure-test")))
    testImplementation(testFixtures(project(":extensions:azure:data-plane:storage")))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation("org.awaitility:awaitility:${awaitility}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-azure-data-factory") {
            artifactId = "data-plane-azure-data-factory"
            from(components["java"])
        }
    }
}
