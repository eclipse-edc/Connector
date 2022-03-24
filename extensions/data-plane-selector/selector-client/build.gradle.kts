/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

val mockitoVersion: String by project
val jodahFailsafeVersion: String by project
val okHttpVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane-selector:selector-spi"))
    implementation(project(":common:util"))
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(project(":extensions:http"))
    testImplementation(project(":extensions:data-plane-selector:selector-api"))
    testImplementation(project(":extensions:api:api-core"))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-selector-client") {
            artifactId = "data-plane-selector-client"
            from(components["java"])
        }
    }
}
