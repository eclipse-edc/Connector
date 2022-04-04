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

//This file serves as BOM for all stores based on memory
dependencies {
    api(project(":spi"))
    api(project(":extensions:in-memory:assetindex-memory"))
    api(project(":extensions:in-memory:contractdefinition-store-memory"))
    api(project(":extensions:in-memory:did-document-store-inmem"))
    api(project(":extensions:in-memory:fcc-node-directory-memory"))
    api(project(":extensions:in-memory:identity-hub-memory"))
    api(project(":extensions:in-memory:negotiation-store-memory"))
    api(project(":extensions:in-memory:transfer-store-memory"))
    api(project(":extensions:in-memory:policy-store-memory"))
}

publishing {
    publications {
        create<MavenPublication>("in-memory") {
            artifactId = "in-memory"
            from(components["java"])
        }
    }
}
