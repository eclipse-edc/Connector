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
}

dependencies {
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:core-spi"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-transform"))

    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:transform-lib"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(libs.restAssured)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockserver.client)

    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
}


