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
    testImplementation(libs.restAssured)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.wiremock)
    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    runtimeOnly(libs.parsson)

    testImplementation(libs.dsp.tck.api)
    testImplementation(libs.dsp.tck.core)
    testImplementation(libs.dsp.tck.runtime)
    testImplementation(libs.dsp.tck.system)
    testRuntimeOnly(libs.dsp.tck.catalog)
    testRuntimeOnly(libs.dsp.tck.contractnegotiation)
    testRuntimeOnly(libs.dsp.tck.metadata)
    testRuntimeOnly(libs.dsp.tck.transferprocess)

    testCompileOnly(project(":system-tests:tck:dsp-tck-connector-under-test"))
}

edcBuild {
    publish.set(false)
}
