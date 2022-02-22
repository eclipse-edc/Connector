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
val okHttpVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":extensions:iam:oauth2:oauth2-spi"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

    testImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(project(":extensions:filesystem:vault-fs"))
    testImplementation(project(":extensions:iam:oauth2:oauth2-core"))

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
}

publishing {
    publications {
        create<MavenPublication>("iam-daps") {
            artifactId = "iam-daps"
            from(components["java"])
        }
    }
}
