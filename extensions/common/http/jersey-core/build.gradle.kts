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

    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))

    implementation(libs.bundles.jersey.core)
    implementation(libs.jetty.jakarta.servlet.api)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:common:runtime-core"))

    testImplementation(libs.restAssured)

    testFixturesApi(project(":core:common:lib:json-ld-lib"))
    testFixturesApi(project(":core:common:junit"))
    testFixturesApi(project(":extensions:common:http:jetty-core"))
    testFixturesApi(project(":extensions:common:json-ld"))
    testFixturesImplementation(project(":extensions:common:http:lib:jersey-providers-lib"))
    testFixturesApi(libs.jakarta.rsApi)
    testFixturesApi(libs.jackson.datatype.jakarta.jsonp)
}


