/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-http-spi"))
    api(project(":spi:common:json-ld-spi"))

    testImplementation(project(":core:common:lib:transform-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-catalog:dsp-catalog-transform"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(libs.restAssured)

    testFixturesApi(project(":core:common:junit"))
    testFixturesImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testFixturesImplementation(libs.restAssured)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.mockito.core)
}