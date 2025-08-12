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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       Mercedes-Benz Tech Innovation GmbH - publish public api context into dedicated swagger hub page
 *
 */


plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:common:lib:util-lib"))

    implementation(project(":core:data-plane:data-plane-util"))
    implementation(libs.jakarta.rsApi)

    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.jersey.multipart)
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
}
edcBuild {
    swagger {
        apiGroup.set("public-api")
    }
}


