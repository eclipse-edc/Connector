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

    // TODO: are all of these necessary?
    testImplementation(libs.dsp.tck.api)
    testImplementation(libs.dsp.tck.core)
    testImplementation(libs.dsp.tck.runtime)
    testImplementation("org.eclipse.dataspacetck.dps:dps-system:1.0.0-SNAPSHOT")
    testRuntimeOnly("org.eclipse.dataspacetck.dps:dps-testcases:1.0.0-SNAPSHOT") // TODO: put in version catalog

    testImplementation(project(":core:common:junit"))
    testRuntimeOnly(project(":system-tests:tck:dps-tck-connector-under-test"))
}

edcBuild {
    publish.set(false)
}
