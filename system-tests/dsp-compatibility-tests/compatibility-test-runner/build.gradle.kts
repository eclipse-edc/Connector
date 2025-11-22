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
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.dsp.tck.core)
    testImplementation(libs.dsp.tck.runtime)
    testImplementation(libs.dsp.tck.api)
    testImplementation(libs.dsp.tck.system)
    testRuntimeOnly(libs.dsp.tck.metadata)
    testRuntimeOnly(libs.dsp.tck.transferprocess)
    testRuntimeOnly(libs.dsp.tck.contractnegotiation)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    runtimeOnly(libs.parsson)

    testCompileOnly(project(":system-tests:dsp-compatibility-tests:connector-under-test"))
}

edcBuild {
    publish.set(false)
}
