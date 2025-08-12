/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:policy:request-policy-context-spi"))
    api(project(":data-protocols:dsp:dsp-spi"))

    api(libs.okhttp)
    api(libs.jakarta.json.api)
    api(libs.jakarta.rsApi)

    testFixturesApi(project(":core:common:junit"))
    testFixturesApi(project(":spi:common:json-ld-spi"))
}
