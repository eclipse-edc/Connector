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
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":extensions:common:azure:azure-blob-core"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation(project(":core:common:util"))

    implementation(libs.azure.storageblob)
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-blob-core")))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-azure-storage") {
            artifactId = "data-plane-azure-storage"
            from(components["java"])
        }
    }
}