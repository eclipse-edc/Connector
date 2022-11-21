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

dependencies {
    testFixturesApi(project(":core:common:junit"))
    testFixturesImplementation(project(":extensions:common:aws:aws-s3-core"))

    testFixturesImplementation(libs.okhttp) // needed for MinIO health probe
    testFixturesImplementation(libs.awaitility)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(libs.aws.s3)
}

publishing {
    publications {
        create<MavenPublication>("aws-s3-test") {
            artifactId = "aws-s3-test"
            from(components["java"])
        }
    }
}
