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
    api(project(":core:common:util"))
    api(project(":spi:federated-catalog:federated-catalog-spi"))
    api(project(":extensions:common:azure:azure-cosmos-core"))

    implementation(
        libs.azure.cosmos
    )
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}


publishing {
    publications {
        create<MavenPublication>("fcc-node-directory-cosmos") {
            artifactId = "fcc-node-directory-cosmos"
            from(components["java"])
        }
    }
}