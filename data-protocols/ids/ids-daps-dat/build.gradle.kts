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


dependencies {
    api(project(":spi"))
    implementation(project(":data-protocols:ids:ids-spi"))

    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.69")
}


publishing {
    publications {
        create<MavenPublication>("data-protocols.ids-daps-dat") {
            artifactId = "data-protocols..ids-daps-dat"
            from(components["java"])
        }
    }
}
