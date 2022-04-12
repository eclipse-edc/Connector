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

val rsApi: String by project
val nimbusVersion: String by project
val jerseyVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:contract-spi"))
    api(project(":spi:transfer-spi"))
    api(project(":spi:web-spi"))
    api(project(":extensions:data-plane-transfer:data-plane-transfer-core"))
    api(project(":common:token-validation-lib"))
    api("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    api("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

    testImplementation("org.glassfish.jersey.media:jersey-media-multipart:${jerseyVersion}")
}


publishing {
    publications {
        create<MavenPublication>("data-plane-transfer-sync") {
            artifactId = "data-plane-transfer-sync"
            from(components["java"])
        }
    }
}
