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

val nimbusVersion: String by project

dependencies {
    api(project(":spi:transfer-spi"))
    api(project(":extensions:token-services:token-services-generation"))
    api(project(":extensions:data-plane:data-plane-http-proxy:data-plane-http-proxy-core"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-http-proxy-provider") {
            artifactId = "data-plane-proxy-http-provider"
            from(components["java"])
        }
    }
}
