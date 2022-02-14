/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

val rsApi: String by project
val mockitoVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:core-spi"))

    implementation(project(":common:util"))
    implementation("com.azure:azure-security-keyvault-secrets:4.2.3")
    implementation("com.azure:azure-identity:1.2.0")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation("com.microsoft.azure:azure-mgmt-resources:1.3.0")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager:2.1.0")
    testImplementation("com.azure:azure-identity:1.2.5")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager-keyvault:2.2.0")
    testImplementation(testFixtures(project(":common:util")))
    testImplementation("org.mockito:mockito-inline:${mockitoVersion}")
}


publishing {
    publications {
        create<MavenPublication>("azure-vault") {
            artifactId = "azure-vault"
            from(components["java"])
        }
    }
}
