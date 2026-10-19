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
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.restAssured)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.wiremock)
    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    runtimeOnly(libs.parsson)

    testImplementation(libs.dps.tck)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:lib:core-lib")))
    // the connectors under test are loaded by the runtime extensions (modules), so that their extensions are not shared
    testCompileOnly(project(":system-tests:tck:dps-tck-connector-under-test"))
    testCompileOnly(project(":system-tests:tck:dps-tck-virtual-connector-under-test"))
}

edcBuild {
    publish.set(false)
}
