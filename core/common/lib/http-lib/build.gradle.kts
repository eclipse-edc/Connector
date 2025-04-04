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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:http-spi"))

    implementation(libs.okhttp)
    implementation(libs.dnsOverHttps)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:json-lib"))
    testImplementation(project(":core:common:lib:util-lib"))
    testImplementation(libs.mockserver.netty)
}


