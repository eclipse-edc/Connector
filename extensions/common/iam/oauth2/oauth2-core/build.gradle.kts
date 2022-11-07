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

val httpMockServer: String by project
val nimbusVersion: String by project
val okHttpVersion: String by project

dependencies {
    api(project(":spi:common:oauth2-spi"))
    implementation(project(":core:common:jwt-core"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(project(":core:common:junit"))

    testImplementation("org.mock-server:mockserver-netty:${httpMockServer}:shaded")
    testImplementation("org.mock-server:mockserver-client-java:${httpMockServer}:shaded")
}

publishing {
    publications {
        create<MavenPublication>("oauth2-core") {
            artifactId = "oauth2-core"
            from(components["java"])
        }
    }
}
