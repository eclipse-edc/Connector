/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    api(project(":spi:dataspace-protocol-spi"))
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-http-spi"))

    implementation(project(":core:common:lib:jsonld-lib"))

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(testFixtures(project(":core:common:lib:jsonld-lib")))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(libs.restAssured)

    testFixturesApi(project(":core:common:junit"))
    testFixturesImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testFixturesImplementation(libs.restAssured)
}
