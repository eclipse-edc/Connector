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

dependencies {
    testFixturesImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testFixturesImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    testImplementation("org.junit-pioneer:junit-pioneer:1.6.2")
}

publishing {
    publications {
        create<MavenPublication>("common-util") {
            artifactId = "common-util"
            from(components["java"])
        }
    }
}
