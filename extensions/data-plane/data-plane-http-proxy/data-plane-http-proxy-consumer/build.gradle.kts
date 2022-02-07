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
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
}

val okHttpVersion: String by project
val nimbusVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":extensions:data-plane:data-plane-spi"))
    api(project(":extensions:token-services:token-services-generation"))
    api(project(":extensions:data-plane:data-plane-http-proxy:data-plane-http-proxy-core"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-http-proxy-consumer") {
            artifactId = "data-plane-http-proxy-consumer"
            from(components["java"])
        }
    }
}
