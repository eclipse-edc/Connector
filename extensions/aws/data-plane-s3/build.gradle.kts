/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

val okHttpVersion: String by project
val storageBlobVersion: String by project;
val jodahFailsafeVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:aws:s3:s3-core"))

    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")

    testImplementation(project(":extensions:data-plane:data-plane-framework"))
    testImplementation(testFixtures(project(":extensions:aws:aws-test")))
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-s3") {
            artifactId = "data-plane-s3"
            from(components["java"])
        }
    }
}
