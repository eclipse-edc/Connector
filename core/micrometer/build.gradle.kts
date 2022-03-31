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

val okHttpVersion: String by project
val micrometerVersion: String by project

dependencies {
    api(project(":core:base"))
    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    api("io.micrometer:micrometer-core:${micrometerVersion}")

    testImplementation(project(":extensions:api:observability"))
    testImplementation(testFixtures(project(":extensions:azure:azure-test")))
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(testFixtures(project(":common:util")))

    testRuntimeOnly(project(":extensions:http:jersey-micrometer"))
    testRuntimeOnly(project(":extensions:http:jetty-micrometer"))
}

tasks.withType<Test> {
    val agent = rootDir.resolve("opentelemetry-javaagent.jar")
    if (agent.exists()) {
        jvmArgs(
            "-javaagent:${agent.absolutePath}",
            // Exposes metrics at http://localhost:9464/metrics
            "-Dotel.metrics.exporter=prometheus"
        );
    }
}

publishing {
    publications {
        create<MavenPublication>("core-micrometer") {
            artifactId = "core-micrometer"
            from(components["java"])
        }
    }
}
