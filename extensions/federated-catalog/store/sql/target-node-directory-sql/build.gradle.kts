/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:crawler-spi"))
    implementation(project(":core:common:lib:sql-lib"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":extensions:common:sql:sql-bootstrapper"))
    implementation(project(":spi:common:transaction-datasource-spi"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(testFixtures(project(":spi:crawler-spi")))
}
