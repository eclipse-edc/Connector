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

val assertj: String by project
val awaitility: String by project
val awsVersion: String by project
val jupiterVersion: String by project
val okHttpVersion: String by project


dependencies {
    testFixturesApi(project(":extensions:common:junit"))
    testFixturesApi(testFixtures(project(":core:common:util")))
    testFixturesImplementation(project(":extensions:common:aws:aws-s3-core"))

    testFixturesImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}") // needed for MinIO health probe
    testFixturesImplementation("org.awaitility:awaitility:${awaitility}")
    testFixturesImplementation("org.assertj:assertj-core:$assertj")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testFixturesApi("software.amazon.awssdk:s3:${awsVersion}")
}

publishing {
    publications {
        create<MavenPublication>("aws-s3-test") {
            artifactId = "aws-s3-test"
            from(components["java"])
        }
    }
}