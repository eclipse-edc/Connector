/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:oauth2-spi"))

    testImplementation(project(":core:common:lib:json-lib"))
    testImplementation(project(":core:common:lib:util-lib"))
    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
    testImplementation(libs.wiremock)
}


