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
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:transaction-spi"))

    implementation(project(":core:common:lib:sql-lib"))
    implementation(project(":extensions:common:sql:sql-bootstrapper"))
    implementation(project(":spi:common:identity-trust-sts-spi"))
    implementation(project(":spi:common:transaction-datasource-spi"))
    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(testFixtures(project(":spi:common:identity-trust-sts-spi")))

}
