/*
*  Copyright (c) 2021 Daimler TSS GmbH
*
*  This program and the accompanying materials are made available under the
*  terms of the Apache License, Version 2.0 which is available at
*  https://www.apache.org/licenses/LICENSE-2.0
*
*  SPDX-License-Identifier: Apache-2.0
*
*  Contributors:
*       Daimler TSS GmbH - Initial API and Implementation
*
*/

val openTelemetryVersion: String by project

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:contract-spi"))

    implementation(project(":common:state-machine-lib"))
    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")

    testImplementation(project(":extensions:in-memory:negotiation-store-memory"))
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("contract") {
            artifactId = "contract"
            from(components["java"])
        }
    }
}
