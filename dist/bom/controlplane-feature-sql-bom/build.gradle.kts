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
    api(project(":extensions:control-plane:store:sql:asset-index-sql"))
    api(project(":extensions:control-plane:store:sql:contract-definition-store-sql"))
    api(project(":extensions:control-plane:store:sql:contract-negotiation-store-sql"))
    api(project(":extensions:control-plane:store:sql:control-plane-sql"))
    api(project(":extensions:control-plane:store:sql:policy-definition-store-sql"))
    api(project(":extensions:control-plane:store:sql:transfer-process-store-sql"))
    api(project(":extensions:control-plane:store:sql:participantcontext-store-sql"))
    api(project(":extensions:common:store:sql:edr-index-sql"))
    api(project(":extensions:common:store:sql:jti-validation-store-sql"))
    api(project(":extensions:data-plane-selector:store:sql:data-plane-instance-store-sql"))
    api(project(":extensions:policy-monitor:store:sql:policy-monitor-store-sql"))

    // other SQL dependencies - not strictly necessary, but could come in handy for BOM users
    api(project(":extensions:common:sql:sql-core"))
    api(project(":extensions:common:sql:sql-bootstrapper"))
    api(project(":extensions:common:sql:sql-lease"))
    api(project(":extensions:common:sql:sql-lease-core"))
    api(project(":extensions:common:sql:sql-pool:sql-pool-apache-commons"))
    api(project(":extensions:common:transaction:transaction-local"))

    // third-party deps
    api(libs.postgres)
}
