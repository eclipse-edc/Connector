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

val nimbusVersion: String by project
val mockitoVersion: String by project

dependencies {
    api(project(":spi:core-spi"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
}


publishing {
    publications {
        create<MavenPublication>("token-validation") {
            artifactId = "token-validation"
            from(components["java"])
        }
    }
}
