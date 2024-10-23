/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:oauth2-spi"))
    implementation(project(":spi:common:keys-spi"))
    implementation(project(":spi:common:jwt-signer-spi"))
    implementation(project(":extensions:common:iam:oauth2:oauth2-client"))
    implementation(project(":core:common:lib:token-lib"))

    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:lib:http-lib")))

    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockserver.client)
}


