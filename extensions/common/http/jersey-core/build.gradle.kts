/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Catena-X Consortium - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:web-spi"))
    api(project(":extensions:common:http:jetty-core"))

    implementation(libs.jakartaJson)
    implementation(libs.jacksonJsonP)
    implementation(libs.bundles.jersey.core)
    implementation(libs.jetty.jakarta.servlet.api)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.restAssured)
    testImplementation(libs.jersey.beanvalidation) //for validation

    testFixturesApi(project(":core:common:junit"))
    testFixturesApi(project(":extensions:common:json-ld"))
    testFixturesApi(libs.jakarta.rsApi)
    testFixturesApi(libs.jacksonJsonP)
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.mockito.core)
}


