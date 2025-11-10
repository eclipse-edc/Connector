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
    api(project(":dist:bom:controlplane-base-bom"))

    // DCP dependencies, JWT and LDP
    api(project(":spi:common:jwt-spi"))

    api(project(":extensions:common:crypto:ldp-verifiable-credentials"))
    api(project(":extensions:common:crypto:jwt-verifiable-credentials"))
    api(project(":extensions:common:crypto:lib:jws2020-lib"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-core"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-issuers-configuration"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-service"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-transform"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-remote-client"))


    api(project(":extensions:common:iam:decentralized-identity"))
    api(project(":extensions:common:iam:oauth2:oauth2-client"))
}