/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:decentralized-claims-spi"))

    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":core:common:lib:sql-lib"))
    implementation(project(":extensions:common:sql:sql-bootstrapper"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:common:decentralized-claims-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
}
