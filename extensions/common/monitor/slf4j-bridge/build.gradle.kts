/*
 *  Copyright (c) 2023 NTT DATA Group Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       NTT DATA Group Corporation - initial implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}


dependencies {
    api(libs.slf4j.api)
    api(project(":spi:common:core-spi"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:junit")))
}
