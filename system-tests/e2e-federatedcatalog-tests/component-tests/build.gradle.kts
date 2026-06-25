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
    testImplementation(project(":core:catalog-crawler:catalog-crawler-core"))
    testImplementation(project(":extensions:federated-catalog:api:federated-catalog-api"))

    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":spi:control-plane-spi"))
    testImplementation(project(":data-protocols:dsp"))
    testImplementation(testFixtures(project(":core:common:lib:jsonld-lib")))
    testImplementation(project(":extensions:common:http:jetty-core"))
    testImplementation(project(":core:common:junit"))

    testImplementation(project(":data-protocols:dsp:dsp-http-spi"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    testImplementation(project(":spi:dataspace-protocol-spi"))

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)

    testRuntimeOnly(project(":extensions:common:iam:iam-mock"))
}

edcBuild {
    publish.set(false)
}
