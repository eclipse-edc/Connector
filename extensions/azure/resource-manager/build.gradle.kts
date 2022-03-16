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

plugins {
    `java-library`
}

dependencies {
    implementation(project(":extensions:azure:data-plane:common"))
    implementation(project(":common:util"))
    implementation("com.azure:azure-identity:${azureIdentityVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-authorization:${azureResourceManagerVersion}")

    testImplementation(testFixtures(project(":extensions:azure:azure-test")))
    testImplementation(testFixtures(project(":extensions:azure:data-plane:storage")))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
}

publishing {
    publications {
        create<MavenPublication>("azure-resource-manager") {
            artifactId = "azure-resource-manager"
            from(components["java"])
        }
    }
}
