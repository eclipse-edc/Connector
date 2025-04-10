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
    api(project(":core:common:edr-store-core"))
    api(project(":core:common:runtime-core"))
    api(project(":core:common:token-core"))
    api(project(":core:common:runtime-core"))
    api(project(":core:control-plane:control-plane-core"))
    api(project(":core:data-plane-selector:data-plane-selector-core"))
    api(project(":core:policy-monitor:policy-monitor-core"))
    api(project(":data-protocols:dsp"))

    // extension dependencies
    api(project(":extensions:common:configuration:configuration-filesystem"))
    api(project(":extensions:common:auth:auth-tokenbased"))
    api(project(":extensions:common:auth:auth-configuration"))
    api(project(":extensions:common:auth:auth-delegated"))
    api(project(":extensions:common:api:api-core"))
    api(project(":extensions:common:api:api-observability"))
    api(project(":extensions:common:api:control-api-configuration"))
    api(project(":extensions:common:api:version-api"))

    api(project(":extensions:common:http"))
    api(project(":extensions:control-plane:api:control-plane-api"))
    api(project(":extensions:control-plane:api:management-api"))
    api(project(":extensions:control-plane:transfer:transfer-data-plane-signaling"))
    api(project(":extensions:data-plane-selector:data-plane-selector-api"))
    api(project(":extensions:data-plane-selector:data-plane-selector-control-api"))
    api(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-client"))
    api(project(":extensions:control-plane:callback:callback-event-dispatcher"))
    api(project(":extensions:control-plane:callback:callback-http-dispatcher"))
    api(project(":extensions:control-plane:edr:edr-store-receiver"))

    // libs
    api(project(":core:common:lib:transform-lib"))
}

edcBuild {

}
