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

val failsafeVersion: String by project
val okHttpVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))

    implementation(project(":common:util"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    api("dev.failsafe:failsafe:${failsafeVersion}")

    testImplementation(project(":extensions:junit"))
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("hashicorp-vault") {
            artifactId = "hashicorp-vault"
            from(components["java"])
        }
    }
}
