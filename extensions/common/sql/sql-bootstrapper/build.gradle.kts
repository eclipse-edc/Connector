/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial build file
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":extensions:common:sql:sql-core")) // SqlQueryExecutor
//
//    testImplementation(project(":core:common:junit"))
//    testImplementation(project(":extensions:common:transaction:transaction-local"))
//    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))
//    testImplementation(libs.postgres)
//    testImplementation(libs.assertj)
//
//    testFixturesImplementation(project(":extensions:common:sql:sql-core"))
}


