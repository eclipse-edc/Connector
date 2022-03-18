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
 *
 */
val rsApi: String by project
val jerseyVersion: String by project
val okHttpVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:web-spi"))
    api(project(":common:token-validation-lib"))
    implementation(project(":extensions:data-plane:data-plane-spi"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    testImplementation("org.glassfish.jersey.media:jersey-media-multipart:${jerseyVersion}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-api") {
            artifactId = "data-plane-api"
            from(components["java"])
        }
    }
}
