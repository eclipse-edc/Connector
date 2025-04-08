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
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:oauth2-spi"))

    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:common:runtime-core"))
    testImplementation(project(":core:common:token-core"))
    testImplementation(project(":extensions:common:iam:oauth2:oauth2-core"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.testcontainers.junit)
}


