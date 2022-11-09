/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
} dependencies {
    testImplementation(project(":core:common:junit"))

    testFixturesImplementation(project(":spi:control-plane:control-plane-spi"))
    testFixturesImplementation(project(":core:common:junit"))
    testFixturesImplementation(project(":extensions:control-plane:api:management-api:transfer-process-api"))
    testFixturesImplementation(project(":extensions:common:api:api-core"))
    testFixturesImplementation(libs.restAssured)
    testFixturesImplementation(libs.awaitility)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.junit.jupiter.api)

    testCompileOnly(project(":samples:04.0-file-transfer:consumer"))
    testCompileOnly(project(":samples:04.0-file-transfer:provider"))
}
