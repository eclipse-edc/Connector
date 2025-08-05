/*
 *  Copyright (c) 2025 Think-it GmbH
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
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:web-spi"))
    implementation(project(":extensions:common:validator:validator-data-address-http-data"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(libs.awaitility)
    testImplementation(libs.wiremock)
    testImplementation(libs.restAssured)
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}



