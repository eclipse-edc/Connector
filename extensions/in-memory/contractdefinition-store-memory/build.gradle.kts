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
 *       Daimler TSS GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    implementation(project(":extensions:dataloading"))
    implementation(project(":common:util"))
}

publishing {
    publications {
        create<MavenPublication>("contractdefinition-store-memory") {
            artifactId = "contractdefinition-store-memory"
            from(components["java"])
        }
    }
}
