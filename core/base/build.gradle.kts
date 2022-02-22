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
val okHttpVersion: String by project
val jodahFailsafeVersion: String by project


dependencies {
    api(project(":spi:core-spi"))

    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    api("net.jodah:failsafe:${jodahFailsafeVersion}")

    testImplementation("org.awaitility:awaitility:4.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
}

publishing {
    publications {
        create<MavenPublication>("core-base") {
            artifactId = "core-base"
            from(components["java"])
        }
    }
}
