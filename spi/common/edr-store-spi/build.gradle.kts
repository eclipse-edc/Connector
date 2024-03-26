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
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:core-spi"))

    // needed by the abstract test spec located in testFixtures
    testFixturesImplementation(libs.bundles.jupiter)
    testFixturesImplementation(libs.mockito.core)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(project(":core:common:junit"))
    testFixturesRuntimeOnly(libs.junit.jupiter.engine)
}


