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

dependencies {

    testFixturesApi(project(":core:common:junit"))
    testFixturesApi(project(":spi:control-plane:control-plane-spi"))
    testFixturesApi(project(":spi:control-plane:contract-spi"))
    testFixturesApi(project(":core:common:util"))
    testFixturesApi(project(":extensions:control-plane:api:management-api:contract-negotiation-api"))
    testFixturesApi(project(":extensions:control-plane:api:management-api:transfer-process-api"))


    testFixturesApi(root.junit.jupiter.api)

    testFixturesImplementation(root.assertj)
    testFixturesImplementation(root.restAssured)
    testFixturesImplementation(root.awaitility)

    testImplementation(root.opentelemetry.api)
    testImplementation(libs.opentelemetry.proto)
    testImplementation(root.awaitility)
    testImplementation(root.mockserver.netty)

    testCompileOnly(project(":system-tests:runtimes:file-transfer-provider"))
    testCompileOnly(project(":system-tests:runtimes:file-transfer-consumer"))
}

val otelDownloadUrl =
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.12.0/opentelemetry-javaagent.jar"

fun download(url: String, destFile: File) {
    ant.invokeMethod("get", mapOf("src" to url, "dest" to destFile))
}

tasks.withType<Test> {
    val otelFile = rootDir.resolve("opentelemetry-javaagent.jar")

    if (!otelFile.exists()) {
        logger.lifecycle("Downloading OpenTelemetry Agent")
        download(otelDownloadUrl, otelFile)
    }
    jvmArgs("-javaagent:${otelFile.absolutePath}", "-Dotel.exporter.otlp.protocol=http/protobuf")
}

tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}

edcBuild {
    publish.set(false)
}
