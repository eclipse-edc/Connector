/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

val awaitility: String by project
val failsafeVersion: String by project
val jupiterVersion: String by project
val okHttpVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:dataloading"))

    testImplementation(project(":extensions:junit"))
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation(project(":core:contract")) // for ContractId
}

publishing {
    publications {
        create<MavenPublication>("core-defaults") {
            artifactId = "core-defaults"
            from(components["java"])
        }
    }
}
