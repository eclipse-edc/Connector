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
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:core-spi"))

    testImplementation(project(":core:common:junit-base"))
    testImplementation(project(":core:common:lib:jsonld-lib"))

    testFixturesApi(project(":core:common:junit"))
    testFixturesImplementation(project(":core:common:junit-base"))
    testFixturesImplementation(testFixtures(project(":core:common:junit")))
    testFixturesImplementation(libs.awaitility)
}


