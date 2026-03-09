/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    api(project(":extensions:federated-catalog:store:sql:federated-catalog-cache-sql"))
    api(project(":extensions:federated-catalog:store:sql:target-node-directory-sql"))
    api(project(":extensions:common:sql:sql-core"))
    api(project(":extensions:common:sql:sql-pool:sql-pool-apache-commons"))
    api(project(":extensions:common:transaction:transaction-local"))
    api(project(":extensions:common:sql:sql-bootstrapper"))
    api(libs.postgres)
}
