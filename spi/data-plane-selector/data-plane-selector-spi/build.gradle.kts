/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:common:lib:util-lib"))

    testImplementation(project(":core:common:lib:json-lib"))

    testFixturesImplementation(testFixtures(project(":core:common:junit")))
    testFixturesImplementation(libs.awaitility)
}



