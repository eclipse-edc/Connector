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
    // entity stores
    api(project(":extensions:data-plane:store:sql:accesstokendata-store-sql"))
    api(project(":extensions:data-plane:store:sql:data-plane-store-sql"))

    // other SQL dependencies - not strictly necessary, but could come in handy for BOM users
    api(project(":extensions:common:sql:sql-core"))
    api(project(":extensions:common:sql:sql-bootstrapper"))
    api(project(":extensions:common:sql:sql-lease"))
    api(project(":extensions:common:sql:sql-pool:sql-pool-apache-commons"))
    api(project(":extensions:common:transaction:transaction-local"))

    // third-party deps
    api(libs.postgres)

}
