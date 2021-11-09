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

val cosmosSdkVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":common:util"))
    api(project((":extensions:catalog:federated-catalog-spi")))
    api(project(":extensions:azure:cosmos-config"))
    implementation("com.azure:azure-cosmos:${cosmosSdkVersion}")

    testImplementation(testFixtures(project(":common:util")))
}


publishing {
    publications {
        create<MavenPublication>("azure.cosmos.fcc-node-dir") {
            artifactId = "azure.cosmos.fcc-node-dir"
            from(components["java"])
        }
    }
}
