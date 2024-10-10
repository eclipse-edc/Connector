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

dependencies {
    api(project(":extensions:common:api:api-core"))
    api(project(":extensions:common:api:management-api-configuration"))
    api(project(":extensions:control-plane:api:management-api:asset-api"))
    api(project(":extensions:control-plane:api:management-api:catalog-api"))
    api(project(":extensions:control-plane:api:management-api:contract-agreement-api"))
    api(project(":extensions:control-plane:api:management-api:contract-definition-api"))
    api(project(":extensions:control-plane:api:management-api:contract-negotiation-api"))
    api(project(":extensions:control-plane:api:management-api:edr-cache-api"))
    api(project(":extensions:control-plane:api:management-api:policy-definition-api"))
    api(project(":extensions:control-plane:api:management-api:transfer-process-api"))
}


