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
 */

plugins {
    `java-library`
}

dependencies {
    runtimeOnly(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-api"))
    runtimeOnly(project(":core:data-plane:data-plane-core"))
    runtimeOnly(project(":extensions:control-plane:api:control-plane-api-client"))
    runtimeOnly(project(":extensions:data-plane:data-plane-http"))
    runtimeOnly(project(":extensions:data-plane:data-plane-control-api"))
    runtimeOnly(project(":extensions:data-plane:data-plane-public-api"))
    runtimeOnly(project(":extensions:common:vault:vault-filesystem"))
}

edcBuild {
    publish.set(false)
}
