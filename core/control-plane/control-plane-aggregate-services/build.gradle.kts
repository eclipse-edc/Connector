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

dependencies {
    implementation(project(":spi:control-plane:control-plane-spi"))
    implementation(project(":core:common:util"))

    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
}

publishing {
    publications {
        create<MavenPublication>("control-plane-aggregate-services") {
            artifactId = "control-plane-aggregate-services"
            from(components["java"])
        }
    }
}
