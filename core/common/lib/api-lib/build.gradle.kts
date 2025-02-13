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

    implementation(project(":core:common:lib:validator-lib"))
    implementation(libs.swagger.annotations.jakarta)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(project(":core:common:lib:transform-lib"))
}


