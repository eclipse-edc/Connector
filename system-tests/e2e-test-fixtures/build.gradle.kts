/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    testFixturesApi(project(":core:common:junit"))
    testFixturesApi(project(":spi:control-plane:control-plane-spi"))
    testFixturesApi(project(":spi:control-plane:contract-spi"))
    testFixturesApi(project(":core:common:util"))
    testFixturesApi(project(":extensions:common:json-ld"))

    testFixturesApi(libs.junit.jupiter.api)

    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.restAssured)
    testFixturesImplementation(libs.awaitility)
}
