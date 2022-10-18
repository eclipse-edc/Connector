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

val cosmosSdkVersion: String by project
val failsafeVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":core:common:util"))
    api(project(":extensions:common:azure:azure-cosmos-core"))

    implementation("com.azure:azure-cosmos:${cosmosSdkVersion}")
    implementation("dev.failsafe:failsafe:${failsafeVersion}")

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(project(":spi:common:core-spi")))

}


publishing {
    publications {
        create<MavenPublication>("asset-index-cosmos") {
            artifactId = "asset-index-cosmos"
            from(components["java"])
        }
    }
}