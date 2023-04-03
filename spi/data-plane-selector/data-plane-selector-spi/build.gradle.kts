/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:aggregate-service-spi"))
    implementation(project(":core:common:util"))


    // needed by the abstract test spec located in testFixtures
    testFixturesImplementation(root.bundles.jupiter)
    testFixturesImplementation(root.mockito.core)
    testFixturesImplementation(root.assertj)
    testFixturesRuntimeOnly(root.junit.jupiter.engine)
}



