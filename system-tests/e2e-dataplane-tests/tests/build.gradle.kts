/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    java
}

dependencies {
    testImplementation(project(":spi:data-plane:data-plane-spi"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:keys-lib"))
    testImplementation(project(":core:common:lib:transform-lib")) // for the transformer registry impl
    testImplementation(project(":core:common:lib:crypto-common-lib"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-transform"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(testFixtures(project(":extensions:control-plane:api:management-api:management-api-test-fixtures")))

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.wiremock)

    testCompileOnly(project(":system-tests:e2e-dataplane-tests:runtimes:data-plane"))
}

edcBuild {
    publish.set(false)
}
