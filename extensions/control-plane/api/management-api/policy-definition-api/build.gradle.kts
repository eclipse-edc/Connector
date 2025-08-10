/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:control-plane:policy-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:policy-model"))

    implementation(project(":core:common:lib:api-lib"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:validator-lib"))
    implementation(project(":extensions:common:api:lib:management-api-lib"))
    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:lib:transform-lib"))
    testImplementation(project(":core:control-plane:control-plane-transform"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:data-plane-selector:data-plane-selector-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":extensions:common:transaction:transaction-local"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(libs.restAssured)
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}


