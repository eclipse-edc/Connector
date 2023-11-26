/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    implementation(libs.nimbus.jwt)

    implementation(project(":spi:common:json-ld-spi"))
    implementation(project(":spi:common:identity-trust-spi"))
    implementation(project(":core:common:util"))


    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:crypto:crypto-core"))
    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(project(":spi:common:identity-did-spi"))
}