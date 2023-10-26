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
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-embedded"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-core"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-remote"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-remote-core"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-api"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-client-configuration"))
}

