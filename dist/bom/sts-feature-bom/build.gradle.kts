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
    application
}

dependencies {
    // SPI dependencies
    api(project(":spi:common:core-spi"))

    // core dependencies
    api(project(":core:common:boot"))
    api(project(":core:common:connector-core"))

    // extension dependencies
    api(project(":extensions:common:http"))
    api(project(":extensions:common:json-ld"))
    api(project(":extensions:common:api:api-observability"))
    api(project(":extensions:common:api:version-api"))

    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-api"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-accounts-api"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-core"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-embedded"))
}