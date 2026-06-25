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
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))

    implementation(project(":core:common:lib:core-lib"))
    implementation(project(":core:control-plane:lib:control-plane-lib"))
    implementation(project(":data-protocols:dsp:dsp-lib"))
    implementation(project(":spi:core-spi"))

    // required for integration test
    testImplementation(project(":data-protocols:dsp:dsp-http-spi"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(libs.restAssured)
    testImplementation(project(":extensions:common:iam:iam-mock"))
    testImplementation(project(":data-protocols:dsp:dsp-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
}

edcBuild {
    swagger {
        apiGroup("management-api")
    }
}
