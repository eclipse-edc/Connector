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

//This file serves as BOM for cosmos db
dependencies {
    api(project(":spi"))
    api(project(":extensions:azure:cosmos:assetindex-cosmos"))
    api(project(":extensions:azure:cosmos:contract-definition-store-cosmos"))
    api(project(":extensions:azure:cosmos:contract-negotiation-store-cosmos"))
    api(project(":extensions:azure:cosmos:cosmos-common"))
    api(project(":extensions:azure:cosmos:fcc-node-directory-cosmos"))
    api(project(":extensions:azure:cosmos:transfer-process-store-cosmos"))
}

publishing {
    publications {
        create<MavenPublication>("cosmos") {
            artifactId = "cosmos"
            from(components["java"])
        }
    }
}
