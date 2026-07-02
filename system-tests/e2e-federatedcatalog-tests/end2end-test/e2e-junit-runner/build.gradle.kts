/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
    testImplementation(project(":spi:control-plane-spi"))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":core:control-plane:control-plane-transform"))
    testImplementation(libs.awaitility)
    testImplementation(project(":data-protocols:dsp:dsp-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.jackson.datatype.jsr310)

    testCompileOnly(project(":extensions:common:iam:iam-mock"))
    testCompileOnly(project(":system-tests:e2e-federatedcatalog-tests:end2end-test:catalog-runtime"))
    testCompileOnly(project(":system-tests:e2e-federatedcatalog-tests:end2end-test:connector-runtime"))
}

edcBuild {
    publish.set(false)
}
