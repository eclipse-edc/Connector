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
val bouncycastleVersion: String by project

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation("org.bouncycastle:bcpkix-jdk15on:${bouncycastleVersion}")

    testImplementation(project(":extensions:iam:decentralized-identity:identity-did-crypto"))
    testImplementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    testImplementation("org.bouncycastle:bcprov-jdk15on:${bouncycastleVersion}")
}

publishing {
    publications {
        create<MavenPublication>("filesystem-vault") {
            artifactId = "filesystem-vault"
            from(components["java"])
        }
    }
}
