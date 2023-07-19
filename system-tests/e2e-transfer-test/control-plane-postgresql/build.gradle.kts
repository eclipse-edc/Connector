/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    runtimeOnly(project(":system-tests:e2e-transfer-test:control-plane"))
    runtimeOnly(project(":extensions:control-plane:store:sql:control-plane-sql"))
    runtimeOnly(project(":extensions:common:sql:sql-pool:sql-pool-apache-commons"))
    runtimeOnly(project(":extensions:common:transaction:transaction-local"))
    runtimeOnly(libs.postgres)
    runtimeOnly(project(":extensions:common:api:management-api-configuration"))
}

edcBuild {
    publish.set(false)
}
