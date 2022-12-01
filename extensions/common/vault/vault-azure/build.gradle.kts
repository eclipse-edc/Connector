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


plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))

    implementation(project(":core:common:util"))
    implementation(libs.azure.keyvault)
    implementation(libs.azure.identity)
    implementation(libs.jakarta.rsApi)

    testImplementation("com.microsoft.azure:azure-mgmt-resources:1.41.4")
    testImplementation(libs.azure.resourcemanager)
    testImplementation(libs.azure.resourcemanager.keyvault)

    testImplementation(libs.mockito.inline)
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
