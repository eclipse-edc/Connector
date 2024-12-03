/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - Initial build file
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    testFixturesImplementation(project(":spi:common:core-spi"))
    testFixturesImplementation(project(":spi:common:transaction-datasource-spi"))
    testFixturesImplementation(project(":spi:common:transaction-spi"))
    testFixturesImplementation(project(":core:common:junit"))
    testFixturesImplementation(project(":core:common:lib:sql-lib"))
    testFixturesImplementation(project(":extensions:common:sql:sql-lease"))

    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.mockito.core)
    testFixturesImplementation(libs.postgres)
    testFixturesImplementation(libs.testcontainers.junit)
    testFixturesImplementation(libs.testcontainers.postgres)
}


