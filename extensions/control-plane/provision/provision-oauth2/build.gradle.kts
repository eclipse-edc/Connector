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
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:common:web-spi"))
    implementation(project(":extensions:common:api:api-core"))

    implementation(libs.okhttp)
    implementation(libs.failsafe.core)
    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.mockserver.netty)
}


publishing {
    publications {
        create<MavenPublication>("provision-oauth2") {
            artifactId = "provision-oauth2"
            from(components["java"])
        }
    }
}
