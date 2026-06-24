/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:core-spi"))


    implementation(project(":spi:core-spi"))
    implementation(project(":core:common:lib:util-lib"))


    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.annotation)
    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:common:junit"))
    testRuntimeOnly(libs.jersey.common) // needs the RuntimeDelegate

    testFixturesImplementation(libs.wiremock) {
        exclude("com.networknt", "json-schema-validator")
    }
    testFixturesImplementation(libs.jakarta.json.api)
    testFixturesImplementation(libs.nimbus.jwt)
}
