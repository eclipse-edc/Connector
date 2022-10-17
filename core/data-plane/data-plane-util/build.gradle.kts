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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

val openTelemetryVersion: String by project

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":core:common:util"))

    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-util") {
            artifactId = "data-plane-util"
            from(components["java"])
        }
    }
}
