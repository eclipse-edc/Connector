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
    api(project(":spi:common:token-spi"))
    implementation(project(":core:common:lib:crypto-common-lib"))
    implementation(project(":core:common:lib:token-lib"))

    implementation(libs.jakarta.rsApi)
    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:keys-lib"))
    testImplementation(libs.wiremock)
    testImplementation(libs.awaitility)
}


