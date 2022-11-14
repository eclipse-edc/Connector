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
    api(project(":spi:common:oauth2-spi"))
    implementation(project(":core:common:jwt-core"))

    implementation(libs.nimbus.jwt)
    implementation(libs.okhttp)
    implementation(libs.failsafe.okhttp)

    testImplementation(project(":core:common:junit"))

    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockserver.client)
}

publishing {
    publications {
        create<MavenPublication>("oauth2-core") {
            artifactId = "oauth2-core"
            from(components["java"])
        }
    }
}
