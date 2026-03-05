/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    testImplementation(project(":core:federated-catalog-core-2025"))
    testImplementation(project(":extensions:federated-catalog:api:federated-catalog-api"))

    testImplementation(project(":spi:common:json-ld-spi"))
    testImplementation(project(":spi:control-plane:catalog-spi"))
    testImplementation(project(":data-protocols:dsp"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":extensions:common:http:jetty-core"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:data-plane-selector:data-plane-selector-spi"))

    testImplementation(project(":data-protocols:dsp:dsp-http-spi"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    testImplementation(project(":spi:common:core-spi"))
    testImplementation(project(":spi:common:policy-model"))
    testImplementation(project(":spi:common:transform-spi"))
    testImplementation(project(":spi:crawler-spi"))
    testImplementation(project(":spi:federated-catalog-spi"))
    testImplementation(project(":spi:common:protocol-spi"))

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)

    testRuntimeOnly(project(":extensions:common:iam:iam-mock"))
}

edcBuild {
    publish.set(false)
}
