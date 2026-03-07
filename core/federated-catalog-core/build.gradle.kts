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
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:control-plane:catalog-spi"))
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":spi:common:participant-context-single-spi"))
    api(project(":core:crawler-core"))
    api(project(":spi:federated-catalog-spi"))
    api(project(":core:common:lib:catalog-util-lib"))
    api(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-transform-lib"))
    api(project(":core:control-plane:control-plane-transform"))
    api(project(":core:common:lib:transform-lib"))
    api(project(":core:common:lib:query-lib"))

    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":data-protocols:dsp:dsp-2025:dsp-http-api-configuration-2025"))
    implementation(project(":spi:common:json-ld-spi"))
    implementation(project(":core:common:lib:json-ld-lib"))
    implementation(project(":core:common:lib:store-lib"))

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(libs.awaitility)

    testImplementation(testFixtures(project(":spi:federated-catalog-spi")))
    testImplementation(testFixtures(project(":spi:crawler-spi")))

    testFixturesImplementation(project(":core:common:lib:json-lib"))
    testFixturesImplementation(project(":core:common:lib:json-ld-lib"))
    testFixturesImplementation(project(":core:control-plane:control-plane-transform"))
    testFixturesImplementation(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-transform-lib"))
    testFixturesImplementation(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025"))
}
