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
    java
}

dependencies {
    testImplementation(libs.awaitility)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.restAssured)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.wiremock)
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:common:decentralized-claims-spi"))
    testImplementation(project(":spi:common:identity-did-spi"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    runtimeOnly(libs.parsson)

    testImplementation(libs.dsp.core)
    testImplementation(libs.dcp.tck.runtime)
    testImplementation(libs.dcp.system)
    testRuntimeOnly(libs.dcp.testcases)

    testCompileOnly(project(":dist:bom:controlplane-dcp-bom"))
}

edcBuild {
    publish.set(false)
}
