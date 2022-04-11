/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
}

val httpMockServer: String by project
val jodahFailsafeVersion: String by project
val okHttpVersion: String by project
val faker: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":extensions:data-plane:data-plane-spi"))
    api(project(":extensions:data-plane-transfer:data-plane-transfer-spi"))
    api(project(":extensions:data-plane-selector:selector-spi"))
    implementation(project(":common:util"))

    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation("org.mock-server:mockserver-netty:${httpMockServer}:shaded")
    testImplementation("org.mock-server:mockserver-client-java:${httpMockServer}:shaded")
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-transfer-client") {
            artifactId = "data-plane-transfer-client"
            from(components["java"])
        }
    }
}
