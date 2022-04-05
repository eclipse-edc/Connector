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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
}

val rsApi: String by project

dependencies {
    implementation(project(":common:util"))
    implementation(project(":extensions:aws"))
    implementation(project(":extensions:sql:asset:index"))
    implementation(project(":extensions:sql:common"))
    implementation(project(":extensions:sql:pool:apache-commons-pool"))
    implementation(project(":extensions:transaction:transaction-local"))
    implementation(project(":extensions:api:data-management"))
    implementation(project(":extensions:dataloading"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}
