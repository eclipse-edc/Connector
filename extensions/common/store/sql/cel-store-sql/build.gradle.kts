/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:cel-spi"))
    api(project(":spi:common:transaction-datasource-spi"))

    implementation(project(":core:common:lib:sql-lib"))
    implementation(project(":extensions:common:sql:sql-bootstrapper"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(testFixtures(project(":spi:common:cel-spi")))

    testImplementation(libs.postgres)
}
