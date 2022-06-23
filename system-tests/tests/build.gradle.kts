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
    `java-test-fixtures`
}

val jupiterVersion: String by project
val gatlingVersion: String by project
val openTelemetryVersion: String by project
val awaitility: String by project
val httpMockServer: String by project

dependencies {
    testFixturesApi("io.gatling.highcharts:gatling-charts-highcharts:${gatlingVersion}") {
        exclude(group = "io.gatling", module = "gatling-jms")
        exclude(group = "io.gatling", module = "gatling-jms-java")
        exclude(group = "io.gatling", module = "gatling-mqtt")
        exclude(group = "io.gatling", module = "gatling-mqtt-java")
        exclude(group = "io.gatling", module = "gatling-jdbc")
        exclude(group = "io.gatling", module = "gatling-jdbc-java")
        exclude(group = "io.gatling", module = "gatling-redis")
        exclude(group = "io.gatling", module = "gatling-redis-java")
        exclude(group = "io.gatling", module = "gatling-graphite")
    }

    testFixturesApi(project(":extensions:junit"))
    testFixturesApi(project(":common:util"))
    testFixturesApi(testFixtures(project(":common:util")))

    testFixturesApi("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("io.opentelemetry:opentelemetry-api:${openTelemetryVersion}")
    testImplementation("io.opentelemetry.proto:opentelemetry-proto:0.18.0-alpha")
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("org.mock-server:mockserver-netty:${httpMockServer}:shaded")

    testCompileOnly(project(":system-tests:runtimes:file-transfer-provider"))
    testCompileOnly(project(":system-tests:runtimes:file-transfer-consumer"))
}

tasks.withType<Test> {
    val agent = rootDir.resolve("opentelemetry-javaagent.jar")
    if (agent.exists()) {
        jvmArgs("-javaagent:${agent.absolutePath}", "-Dotel.exporter.otlp.protocol=http/protobuf");
    }
}

tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}
