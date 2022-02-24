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

val okHttpVersion: String by project
val jodahFailsafeVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:transfer-spi"))
    implementation(project(":common:util"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")
}


publishing {
    publications {
        create<MavenPublication>("http-receiver") {
            artifactId = "http-receiver"
            from(components["java"])
        }
    }
}
