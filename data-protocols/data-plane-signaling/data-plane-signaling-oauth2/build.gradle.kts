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
}

dependencies {
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:oauth2-spi"))
    api(project(":spi:common:keys-spi"))
    api(project(":data-protocols:data-plane-signaling:data-plane-signaling-spi"))

    implementation(project(":core:common:lib:keys-lib"))
    implementation(project(":core:common:lib:token-lib"))
    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:keys-lib"))
    testImplementation(project(":core:common:lib:crypto-common-lib"))
    testImplementation(libs.wiremock)
}
