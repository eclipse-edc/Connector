/*
 *  Copyright (c) 2025 Eclipse EDC Contributors
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Eclipse EDC Contributors - Data Masking Extension
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:transform-spi"))
    api(project(":core:common:lib:transform-lib"))
    api(project(":core:common:lib:json-lib"))
    
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito.core)
}
