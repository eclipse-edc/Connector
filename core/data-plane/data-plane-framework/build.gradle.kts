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
    api(project(":spi:common:core-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":common:util"))
    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")
    testImplementation(project(":extensions:common:junit"))
}


publishing {
    publications {
        create<MavenPublication>("data-plane-framework") {
            artifactId = "data-plane-framework"
            from(components["java"])
        }
    }
}
