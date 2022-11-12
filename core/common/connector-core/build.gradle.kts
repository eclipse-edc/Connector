/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

val awaitility: String by project
val failsafeVersion: String by project
val jupiterVersion: String by project
val okHttpVersion: String by project
val bouncycastleVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:policy-engine-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:transaction-datasource-spi"))

    implementation(project(":core:common:policy-engine"))
    implementation(project(":core:common:util"))

    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    api("dev.failsafe:failsafe:${failsafeVersion}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${bouncycastleVersion}")

    testImplementation(project(":core:common:junit"))
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
}

publishing {
    publications {
        create<MavenPublication>("connector-core") {
            artifactId = "connector-core"
            from(components["java"])
        }
    }
}
