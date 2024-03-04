/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

plugins {
    java
}

dependencies {

    testImplementation(project(":spi:data-plane:data-plane-spi"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:control-plane:api:management-api:management-api-test-fixtures")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(libs.jakartaJson)

    testImplementation(libs.restAssured)
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockserver.client)
    testImplementation(project(":core:common:transform-core")) // for the transformer registry impl

    implementation(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-transform"))
}

edcBuild {
    publish.set(false)
}
