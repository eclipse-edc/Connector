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

val nimbusVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":extensions:token:token-validation"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("oauth2-spi") {
            artifactId = "oauth2-spi"
            from(components["java"])
        }
    }
}
