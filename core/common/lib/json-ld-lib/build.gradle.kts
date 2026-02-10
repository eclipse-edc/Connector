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
    api(libs.jakarta.json.api)
    api(libs.jackson.datatype.jakarta.jsonp)
    api(libs.titaniumJsonLd)
    implementation(libs.jackson.datatype.jsr310)

    implementation(project(":core:common:lib:validator-lib"))
    implementation(project(":spi:common:core-spi"))
    implementation(project(":spi:common:json-ld-spi"))
    testImplementation(project(":core:common:lib:util-lib"))
    testImplementation(project(":core:common:junit-base"))

    testImplementation(libs.wiremock)
}
