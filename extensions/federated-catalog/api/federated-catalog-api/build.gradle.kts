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
    api(project(":spi:federated-catalog-spi"))
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:contract-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))

    implementation(project(":spi:common:boot-spi"))
    implementation(project(":core:common:lib:api-lib"))
    implementation(project(":core:common:lib:catalog-util-lib"))
    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))
    implementation(project(":spi:common:transform-spi"))
    implementation(project(":spi:common:web-spi"))


    runtimeOnly(project(":spi:common:json-ld-spi"))
    runtimeOnly(project(":core:common:lib:json-ld-lib"))

    // required for integration test
    testImplementation(project(":data-protocols:dsp:dsp-http-spi"))
    testImplementation(project(":core:common:lib:boot-lib"))
    testImplementation(testFixtures(project(":core:federated-catalog-core")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:json-lib"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(libs.restAssured)
    testImplementation(project(":extensions:common:iam:iam-mock"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-transform-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(project(":core:common:lib:transform-lib"))
    testImplementation(project(":core:common:lib:query-lib"))
}

edcBuild {
    swagger {
        apiGroup.set("catalog-api")
    }
}
