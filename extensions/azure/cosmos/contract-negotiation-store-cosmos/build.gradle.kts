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
val awaitility: String by project

dependencies {
    api(project(":spi:contract-spi"))
    api(project(":common:util"))
    api(project(":extensions:azure:cosmos:cosmos-common"))
    api(project(":extensions:dataloading"))

    implementation("com.azure:azure-cosmos:${cosmosSdkVersion}")
    implementation("dev.failsafe:failsafe:${failsafeVersion}")

    testImplementation(project(":core:contract"))

    testImplementation(testFixtures(project(":extensions:azure:azure-test")))
    testImplementation(testFixtures(project(":extensions:azure:cosmos:cosmos-common")))
    testImplementation("org.awaitility:awaitility:${awaitility}")
}


publishing {
    publications {
        create<MavenPublication>("contract-negotiation-store-cosmos") {
            artifactId = "contract-negotiation-store-cosmos"
            from(components["java"])
        }
    }
}
