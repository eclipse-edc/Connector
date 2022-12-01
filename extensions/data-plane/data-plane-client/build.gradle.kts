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
 *
 */


plugins {
    `java-library`
}

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    implementation(project(":core:common:util"))

    implementation(libs.failsafe.core)
    implementation(libs.okhttp)
    implementation(libs.opentelemetry.annotations)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.restAssured)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockserver.client)
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
