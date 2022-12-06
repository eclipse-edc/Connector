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
    implementation(project(":system-tests:e2e-transfer-test:control-plane"))
    implementation(project(":extensions:control-plane:store:sql:control-plane-sql"))
    implementation(project(":extensions:common:sql:sql-pool:sql-pool-apache-commons"))
    implementation(project(":extensions:common:transaction:transaction-local"))
    implementation(libs.postgres)
}