/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
    `java-test-fixtures`
    `maven-publish`
}

val jupiterVersion: String by project
val mockitoVersion: String by project
val okHttpVersion: String by project
val assertj: String by project

dependencies {
    api(project(":spi"))
    api(project(":core:boot"))
    api(project(":core:base"))
    api(project(":common:util"))
    implementation("org.mockito:mockito-core:${mockitoVersion}")
    implementation("org.assertj:assertj-core:${assertj}")

    implementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("org.junit-pioneer:junit-pioneer:1.6.2")
}

publishing {
    publications {
        create<MavenPublication>("junit") {
            artifactId = "junit"
            from(components["java"])
        }
    }
}
