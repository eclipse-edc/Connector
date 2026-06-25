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
    `maven-publish`
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    api(libs.jakarta.json.api)
    api(libs.jackson.datatype.jakarta.jsonp)
    api(libs.titaniumJsonLd)

    implementation(libs.jackson.datatype.jsr310)

    testImplementation(project(":core:common:junit-base"))
    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    testImplementation(libs.wiremock)

    testFixturesApi(project(":spi:core-spi"))
}
