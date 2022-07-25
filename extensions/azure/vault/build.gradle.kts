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
val azureIdentityVersion: String by project
val azureResourceManagerVersion: String by project
val azureKeyVaultVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:core-spi"))

    implementation(project(":common:util"))
    implementation("com.azure:azure-security-keyvault-secrets:${azureKeyVaultVersion}")
    implementation("com.azure:azure-identity:${azureIdentityVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation("com.microsoft.azure:azure-mgmt-resources:1.41.4")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager:${azureResourceManagerVersion}")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager-keyvault:${azureResourceManagerVersion}")

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
