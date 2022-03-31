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

val openTelemetryVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:transfer-spi"))
    api(project(":common:state-machine-lib"))
    api(project(":common:util"))

    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")

    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
    testImplementation(testFixtures(project(":common:util")))
}


publishing {
    publications {
        create<MavenPublication>("transfer") {
            artifactId = "transfer"
            from(components["java"])
        }
    }
}
