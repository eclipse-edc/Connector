/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val jupiterVersion: String by project
val testContainersVersion: String by project

dependencies {
    implementation(project(":spi"))
    implementation(project(":common:libraries:clients:postgresql"))

    testImplementation("ch.qos.logback:logback-classic:1.2.6")
    testImplementation("org.testcontainers:postgresql:${testContainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}

publishing {
    publications {
        create<MavenPublication>("postgresql-asset-repository") {
            artifactId = "postgresql-asset-repository"
            from(components["java"])
        }
    }
}
