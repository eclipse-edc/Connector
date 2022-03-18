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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

plugins {
    `java-library`
}

val gatlingVersion: String by project

dependencies {
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:${gatlingVersion}") {
        exclude(group = "io.gatling", module="gatling-jms")
        exclude(group = "io.gatling", module="gatling-jms-java")
        exclude(group = "io.gatling", module="gatling-mqtt")
        exclude(group = "io.gatling", module="gatling-mqtt-java")
        exclude(group = "io.gatling", module="gatling-jdbc")
        exclude(group = "io.gatling", module="gatling-jdbc-java")
        exclude(group = "io.gatling", module="gatling-redis")
        exclude(group = "io.gatling", module="gatling-redis-java")
        exclude(group = "io.gatling", module="gatling-graphite")
    }

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))

    testCompileOnly(project(":system-tests:runtimes:file-transfer-provider"))
    testCompileOnly(project(":system-tests:runtimes:file-transfer-consumer"))
}

tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}
