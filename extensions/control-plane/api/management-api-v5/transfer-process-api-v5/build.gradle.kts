/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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
    id(libs.plugins.swagger.get().pluginId)

}

dependencies {
    api(project(":spi:common:auth-spi"))
    api(project(":spi:control-plane:control-plane-spi"))

    implementation(project(":core:common:lib:api-lib"))
    implementation(project(":core:common:lib:validator-lib"))
    implementation(project(":core:control-plane:control-plane-transform"))
    implementation(project(":extensions:common:api:lib:management-api-lib"))
    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))
    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.annotation)

    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}