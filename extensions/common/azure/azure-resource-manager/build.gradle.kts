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

plugins {
    `java-library`
}

dependencies {
    implementation(project(":extensions:common:azure:azure-blob-core"))
    implementation(project(":core:common:util"))
    implementation(libs.azure.identity)
    implementation(libs.azure.resourcemanager)
    implementation(libs.azure.resourcemanager.authorization)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))

    testImplementation(project(":core:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
