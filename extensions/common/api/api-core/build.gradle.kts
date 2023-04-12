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

    implementation(project(":core:common:util"))
    implementation(root.jakarta.rsApi)
    implementation(root.jakarta.validation)
    implementation(root.jersey.beanvalidation) //for validation

    testImplementation(root.jersey.common)
    testImplementation(root.jersey.server)

    testImplementation(project(":core:common:junit"))
}


