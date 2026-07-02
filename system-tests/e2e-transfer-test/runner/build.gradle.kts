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
    testImplementation(project(":spi:control-plane-spi"))
    testImplementation(project(":extensions:common:sql:sql-core"))
    testImplementation(project(":extensions:common:transaction:transaction-local"))

    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:lib:core-lib")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(testFixtures(project(":extensions:control-plane:api:management-api:management-api-test-fixtures")))
    testImplementation(testFixtures(project(":spi:decentralized-claims-spi")))
    testImplementation(testFixtures(project(":data-protocols:data-plane-signaling:data-plane-signaling-spi")))

    testImplementation(project(":extensions:common:json-ld"))

    testImplementation(libs.awaitility)
    testImplementation(libs.postgres)
    testImplementation(libs.restAssured)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.vault)
    testImplementation(libs.wiremock) {
        exclude("com.networknt", "json-schema-validator")
    }
    testImplementation(libs.nimbus.jwt)

    testCompileOnly(project(":system-tests:e2e-transfer-test:control-plane"))
    testCompileOnly(project(":system-tests:e2e-transfer-test:signaling-data-plane"))
}

edcBuild {
    publish.set(false)
}
