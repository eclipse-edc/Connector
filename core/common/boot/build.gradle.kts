/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(libs.slf4j.api)

    implementation(libs.opentelemetry.api)

    testImplementation(libs.junit.jupiter.api)
}

publishing {
    publications {
        create<MavenPublication>("boot") {
            artifactId = "boot"
            from(components["java"])
        }
    }
}