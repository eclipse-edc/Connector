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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:verifiable-credentials-spi"))
    api(project(":spi:common:http-spi"))
    implementation(project(":spi:common:token-spi"))
    implementation(project(":spi:common:identity-did-spi"))
    implementation(project(":extensions:common:iam:decentralized-claims:lib:verifiable-credentials-lib"))
    implementation(libs.jsonschema)

    testImplementation(project(":core:common:junit-base"))
    testImplementation(testFixtures(project(":spi:common:verifiable-credentials-spi")))
}

