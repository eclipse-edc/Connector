/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:decentralized-claims-spi"))
    api(project(":spi:common:identity-did-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:jwt-spi"))
    implementation(project(":core:common:lib:util-lib"))

    implementation(libs.jakarta.rsApi)
    implementation(libs.jsonschema)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:common:decentralized-claims-spi"))) //test functions
    testImplementation(testFixtures(project(":spi:common:verifiable-credentials-spi")))
    testImplementation(project(":core:common:lib:http-lib"))
    testImplementation(project(":core:common:lib:util-lib"))
    testImplementation(libs.wiremock)
}
