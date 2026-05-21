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
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:common:participant-context-single-spi"))
    api(project(":spi:federated-catalog-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    implementation(project(":core:common:lib:catalog-util-lib"))
    implementation(project(":core:common:lib:json-ld-lib"))
    implementation(project(":core:common:lib:query-lib"))
    implementation(project(":core:common:lib:store-lib"))
    implementation(project(":core:common:lib:util-lib"))

    testImplementation(libs.awaitility)
    testImplementation(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-transform-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:transform-lib"))
    testImplementation(project(":core:control-plane:control-plane-transform"))
    testImplementation(testFixtures(project(":spi:federated-catalog-spi")))
    testImplementation(testFixtures(project(":spi:crawler-spi")))
}
