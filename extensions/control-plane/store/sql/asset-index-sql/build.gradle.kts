/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:control-plane:asset-spi"))

    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":core:common:lib:sql-lib"))
    implementation(project(":extensions:common:sql:sql-bootstrapper"))
    implementation(project(":core:common:lib:util-lib"))

    testImplementation(project(":core:common:junit"))

    testImplementation(project(":spi:common:policy-model"))
    testImplementation(libs.assertj)
    testImplementation(testFixtures(project(":spi:control-plane:asset-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(libs.postgres)

    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
}


