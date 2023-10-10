/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:policy-engine-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:transaction-datasource-spi"))
    api(project(":spi:common:transform-spi"))
    api(project(":spi:common:validator-spi"))

    implementation(project(":core:common:policy-engine"))
    implementation(project(":core:common:state-machine"))
    implementation(project(":core:common:transform-core"))
    implementation(project(":core:common:util"))

    implementation(libs.dnsOverHttps)
    implementation(libs.bouncyCastle.bcpkixJdk18on)
    implementation(libs.nimbus.jwt)
    
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockserver.netty)
}


