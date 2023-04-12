/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api(root.bundles.jackson)
    api(project(":spi:common:policy-model"))

    implementation(root.opentelemetry.api)

    testImplementation(project(":core:common:junit"))

    // needed by the abstract test spec located in testFixtures
    testFixturesImplementation(root.bundles.jupiter)
    testFixturesRuntimeOnly(root.junit.jupiter.engine)
    testFixturesImplementation(root.mockito.core)
    testFixturesImplementation(root.assertj)
}


