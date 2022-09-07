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
    api(project(":core:common:base"))
    api(project(":core:common:boot"))
    api(project(":core:common:policy-engine"))
    api(project(":core:control-plane:contract"))
    api(project(":core:control-plane:transfer"))
    implementation(project(":core:common:util"))
}

publishing {
    publications {
        create<MavenPublication>("control-plane-core") {
            artifactId = "control-plane-core"
            from(components["java"])
        }
    }
}
