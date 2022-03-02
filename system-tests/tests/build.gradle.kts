/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
}

val gatlingVersion: String by project

dependencies {
    testImplementation("io.gatling:gatling-http-java:${gatlingVersion}")
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:${gatlingVersion}")

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))

    testRuntimeOnly(project(":system-tests:runtimes:file-transfer-provider"))
    testRuntimeOnly(project(":system-tests:runtimes:file-transfer-consumer"))
}

tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}
