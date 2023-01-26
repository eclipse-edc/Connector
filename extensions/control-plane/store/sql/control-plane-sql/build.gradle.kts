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
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:common:sql:sql-core"))
    implementation(project(":extensions:common:sql:sql-lease"))
    implementation(project(":extensions:control-plane:store:sql:asset-index-sql"))
    implementation(project(":extensions:control-plane:store:sql:contract-definition-store-sql"))
    implementation(project(":extensions:control-plane:store:sql:contract-negotiation-store-sql"))
    implementation(project(":extensions:control-plane:store:sql:policy-definition-store-sql"))
    implementation(project(":extensions:control-plane:store:sql:transfer-process-store-sql"))
}


