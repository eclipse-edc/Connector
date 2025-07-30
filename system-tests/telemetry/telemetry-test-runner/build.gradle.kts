/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    testImplementation(project(":spi:common:json-ld-spi"))
    testImplementation(project(":spi:control-plane:catalog-spi"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":core:common:connector-core"))

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.wiremock)
    testImplementation(libs.opentelemetry.proto)

    testCompileOnly(project(":system-tests:telemetry:telemetry-test-runtime"))
}

edcBuild {
    publish.set(false)
}

tasks.withType<Test> {
    val download = { url: String, destFile: File -> ant.invokeMethod("get", mapOf("src" to url, "dest" to destFile)) }

    val agentFile = rootDir.resolve("opentelemetry-javaagent.jar")

    if (!agentFile.exists()) {
        logger.lifecycle("Downloading OpenTelemetry Agent")
        download(
            "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.27.0/opentelemetry-javaagent.jar",
            agentFile
        )
    }
    jvmArgs(
        "-javaagent:${agentFile.absolutePath}",
        "-Dotel.exporter.otlp.protocol=http/protobuf",
        // Exposes metrics at http://localhost:9464/metrics
        "-Dotel.metrics.exporter=prometheus"
    )
}

