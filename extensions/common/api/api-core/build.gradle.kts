/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */
plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:aggregate-service-spi"))
    api(project(":spi:common:transform-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:common:json-ld-spi"))

    implementation(project(":core:common:util"))
    implementation(project(":core:common:validator-core"))
    implementation(libs.jakarta.rsApi)
    implementation(libs.swagger.annotations.jakarta)

    testImplementation(libs.jersey.common)
    testImplementation(libs.jersey.server)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:json-ld"))
}


