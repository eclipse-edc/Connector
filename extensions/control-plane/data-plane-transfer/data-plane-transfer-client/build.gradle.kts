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
val failsafeVersion: String by project
val okHttpVersion: String by project
val openTelemetryVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":spi:control-plane:data-plane-transfer-spi"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    implementation(project(":common:util"))

    implementation("dev.failsafe:failsafe:${failsafeVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")

    testImplementation("org.mock-server:mockserver-netty:${httpMockServer}:shaded")
    testImplementation("org.mock-server:mockserver-client-java:${httpMockServer}:shaded")
    testImplementation(project(":extensions:common:junit"))

}

publishing {
    publications {
        create<MavenPublication>("data-plane-transfer-client") {
            artifactId = "data-plane-transfer-client"
            from(components["java"])
        }
    }
}
