/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:transaction-datasource-spi"))

    implementation(project(":core:common:lib:http-lib"))
    implementation(project(":core:common:lib:json-lib"))
    implementation(project(":core:common:lib:query-lib"))
    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":core:common:lib:validator-lib"))

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
}


