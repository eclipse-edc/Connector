/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}
dependencies {
    implementation(project(":spi:common:core-spi"))

    implementation(libs.okhttp)
    implementation(libs.cloudEvents)
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":core:common:junit")))
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.awaitility)
}

publishing {
    publications {
        create<MavenPublication>("events-cloud-http") {
            artifactId = "events-cloud-http"
            from(components["java"])
        }
    }
}
