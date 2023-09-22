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
    api(project(":spi:common:core-spi"))
    api(project(":core:common:boot"))
    api(project(":core:common:connector-core"))
    api(project(":core:common:util"))

    implementation(libs.mockito.core)
    implementation(libs.assertj)
    implementation(libs.junit.jupiter.api)
    runtimeOnly(libs.junit.jupiter.engine)

    implementation(libs.junit.pioneer)
    implementation(libs.testcontainers.junit)
}


