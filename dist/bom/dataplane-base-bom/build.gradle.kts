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
    // SPI dependencies
    api(project(":spi:common:core-spi"))

    // core dependencies
    api(project(":core:common:boot"))
    api(project(":core:common:connector-core"))
    api(project(":core:common:token-core"))
    api(project(":core:data-plane:data-plane-core"))


    // extension dependencies
    api(project(":extensions:common:api:control-api-configuration"))
    api(project(":extensions:common:configuration:configuration-filesystem"))
    api(project(":extensions:common:json-ld"))
    api(project(":extensions:control-plane:api:control-plane-api-client"))
    api(project(":extensions:data-plane:data-plane-self-registration"))
    api(project(":extensions:data-plane:data-plane-http"))
    api(project(":extensions:data-plane:data-plane-http-oauth2"))
    api(project(":extensions:data-plane:data-plane-public-api-v2"))
    api(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-api"))
    api(project(":extensions:data-plane:data-plane-iam"))
    api(project(":extensions:data-plane-selector:data-plane-selector-client"))
    api(project(":extensions:common:api:api-observability"))
    api(project(":extensions:common:http"))
}