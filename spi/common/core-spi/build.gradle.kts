/*
 *  Copyright (c) 2021 Microsoft Corporation
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

val jacksonVersion: String by project
val openTelemetryVersion: String by project
val jupiterVersion: String by project
val mockitoVersion: String by project
val assertj: String by project

plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    api("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
    api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

    api(project(":spi:common:policy-model"))

    implementation("io.opentelemetry:opentelemetry-api:${openTelemetryVersion}")

    testImplementation(project(":extensions:common:junit"))

    // needed by the abstract test spec located in testFixtures
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testFixturesImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testFixturesImplementation("org.assertj:assertj-core:${assertj}")
}

publishing {
    publications {
        create<MavenPublication>("core-spi") {
            artifactId = "core-spi"
            from(components["java"])
        }
    }
}
