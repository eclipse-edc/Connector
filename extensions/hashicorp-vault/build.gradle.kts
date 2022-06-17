/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

val mockitoVersion: String by project
val okHttpVersion: String by project
val bouncycastleVersion: String by project
val testContainersVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:core-spi"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${bouncycastleVersion}")

    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation("org.mockito:mockito-inline:${mockitoVersion}")
    testImplementation("org.testcontainers:vault:${testContainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")
}


publishing {
    publications {
        create<MavenPublication>("hashicorp-vault") {
            artifactId = "hashicorp-vault"
            from(components["java"])
        }
    }
}
