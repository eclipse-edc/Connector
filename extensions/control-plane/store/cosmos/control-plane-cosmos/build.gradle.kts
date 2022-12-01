/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

//This file serves as BOM for cosmos db for control plane
dependencies {
    api(project(":extensions:control-plane:store:cosmos:asset-index-cosmos"))
    api(project(":extensions:control-plane:store:cosmos:contract-definition-store-cosmos"))
    api(project(":extensions:control-plane:store:cosmos:contract-negotiation-store-cosmos"))
    api(project(":extensions:control-plane:store:cosmos:policy-definition-store-cosmos"))
    api(project(":extensions:common:azure:azure-cosmos-core"))
    api(project(":extensions:control-plane:store:cosmos:transfer-process-store-cosmos"))
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
