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
    api(project(":spi:common:transaction-datasource-spi"))

    implementation(project(":core:common:lib:sql-lib"))

    implementation(libs.apache.commons.pool)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:transaction:transaction-local"))

    // required for statically mocking the JDBC DriverManager
    testImplementation(libs.mockito.inline)
    testImplementation(project(":core:common:lib:boot-lib")) //in-mem vault
}


