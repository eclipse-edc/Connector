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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(libs.micrometer)
    api(libs.okhttp)
}

tasks.withType<Test> {
    val agent = rootDir.resolve("opentelemetry-javaagent.jar")
    if (agent.exists()) {
        jvmArgs(
            "-javaagent:${agent.absolutePath}",
            // Exposes metrics at http://localhost:9464/metrics
            "-Dotel.metrics.exporter=prometheus"
        )
    }
}


