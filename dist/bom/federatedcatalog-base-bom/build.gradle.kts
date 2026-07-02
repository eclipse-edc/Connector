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

    api(project(":core:catalog-crawler:catalog-crawler-core"))
    api(project(":core:common:boot"))
    api(project(":core:common:connector-core"))
    api(project(":core:common:lib:core-lib"))
    api(project(":core:common:participant-context-config-core"))
    api(project(":core:common:participant-context-connector-classic-core"))
    api(project(":core:common:participant-context-connector-core"))
    api(project(":core:common:participant-context-core"))
    api(project(":core:common:runtime-core"))
    api(project(":core:common:token-core"))

    api(project(":data-protocols:dsp:dsp-2025:dsp-http-api-configuration-2025"))
    api(project(":data-protocols:dsp:dsp-core:dsp-catalog-http-dispatcher"))
    api(project(":data-protocols:dsp:dsp-core:dsp-http-api-base-configuration"))
    api(project(":data-protocols:dsp:dsp-core:dsp-http-core"))

    api(project(":extensions:common:api:api-core"))
    api(project(":extensions:common:api:api-observability"))
    api(project(":extensions:common:api:management-api-configuration"))
    api(project(":extensions:common:api:version-api"))
    api(project(":extensions:common:auth:auth-configuration"))
    api(project(":extensions:common:auth:auth-delegated"))
    api(project(":extensions:common:auth:auth-tokenbased"))
    api(project(":extensions:common:configuration:configuration-filesystem"))
    api(project(":extensions:common:console-monitor"))
    api(project(":extensions:common:http"))
    api(project(":extensions:common:http:jersey-core"))
    api(project(":extensions:common:http:jetty-core"))
    api(project(":extensions:federated-catalog:api:federated-catalog-api"))

    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))
}
