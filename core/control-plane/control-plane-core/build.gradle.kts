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
    api(project(":spi:control-plane:control-plane-spi"))
    implementation(project(":core:common:base"))
    implementation(project(":core:common:boot"))
    implementation(project(":core:common:policy-engine"))
    implementation(project(":core:control-plane:contract"))
    implementation(project(":core:control-plane:transfer"))
    implementation(project(":common:util"))

    testImplementation(project(":extensions:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("control-plane-core") {
            artifactId = "control-plane-core"
            from(components["java"])
        }
    }
}
