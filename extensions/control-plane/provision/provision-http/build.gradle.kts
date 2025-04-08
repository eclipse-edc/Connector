/*
 *  Copyright (c) 2022 Microsoft Corporation
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
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:web-spi"))
    implementation(project(":extensions:common:validator:validator-data-address-http-data"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:common:runtime-core"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:data-plane-selector:data-plane-selector-core"))
    testImplementation(project(":extensions:common:api:api-core"))
    testImplementation(project(":extensions:common:api:management-api-configuration"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}



