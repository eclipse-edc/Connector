/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    `maven-publish`
}

dependencies {
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-core"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-issuers-configuration"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-service"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-transform"))
}

