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

plugins {
    `java-library`
}

val okHttpVersion: String by project
val jodahFailsafeVersion: String by project

dependencies {
    api(project(":spi:transfer-spi"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(project(":core:transfer"))
    testImplementation(project(":extensions:in-memory:assetindex-memory"))
    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
    testImplementation(project(":extensions:dataloading"))
    testImplementation(testFixtures(project(":launchers:junit")))

}


publishing {
    publications {
        create<MavenPublication>("http-provisioner") {
            artifactId = "http-provisioner"
            from(components["java"])
        }
    }
}
