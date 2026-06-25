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
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    implementation(project(":core:control-plane:lib:control-plane-lib"))
    implementation(project(":core:common:lib:jsonld-lib"))
    implementation(project(":core:common:lib:core-lib"))

    testImplementation(testFixtures(project(":core:common:lib:jsonld-lib")))
    testImplementation(libs.awaitility)
    testImplementation(project(":data-protocols:dsp:dsp-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":core:control-plane:control-plane-transform"))
    testImplementation(testFixtures(project(":spi:control-plane-spi")))
}
