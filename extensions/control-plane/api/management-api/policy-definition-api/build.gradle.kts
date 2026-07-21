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
    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))

    implementation(project(":core:common:lib:core-lib"))
    implementation(project(":core:common:lib:jsonld-lib"))
    implementation(project(":core:control-plane:control-plane-transform"))


    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":core:control-plane:control-plane-transform"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":extensions:common:transaction:transaction-local"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(libs.restAssured)
}

edcBuild {
    swagger {
        apiGroup("management-api")
    }
}


