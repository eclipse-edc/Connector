/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
}

dependencies {
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:control-plane:control-plane-api-client-spi"))

    implementation(libs.failsafe.core)

    testImplementation(project(":core:common:runtime-core"))
    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:data-plane:data-plane-core"))
    testImplementation(project(":core:data-plane-selector:data-plane-selector-core"))
    testImplementation(project(":extensions:control-plane:api:control-plane-api"))
    testImplementation(project(":extensions:common:api:api-core"))
    testImplementation(project(":extensions:common:api:control-api-configuration"))
    testImplementation(project(":extensions:common:auth:auth-tokenbased"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-client"))
    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
    testImplementation(libs.awaitility)


}


