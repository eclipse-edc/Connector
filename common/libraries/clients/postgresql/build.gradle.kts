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
 *       Daimler TSS GmbH - initial build file
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val jupiterVersion: String by project
val postgresqlVersion: String by project
val apacheCommonsPool2Version: String by project
val testContainersVersion: String by project

dependencies {
    implementation("org.postgresql:postgresql:${postgresqlVersion}")
    implementation("org.apache.commons:commons-pool2:${apacheCommonsPool2Version}")

    testImplementation("ch.qos.logback:logback-classic:1.2.6")
    testImplementation("org.testcontainers:postgresql:${testContainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")
}

publishing {
    publications {
        create<MavenPublication>("common-clients-postgresql") {
            artifactId = "common-clients-postgresql"
            from(components["java"])
        }
    }
}
