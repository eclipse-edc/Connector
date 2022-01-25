/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

val jupiterVersion: String by project
val slf4jVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":core:base"))
    api("org.slf4j:slf4j-api:${slf4jVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
}

publishing {
    publications {
        create<MavenPublication>("core-boot") {
            artifactId = "core-boot"
            from(components["java"])
        }
    }
}
