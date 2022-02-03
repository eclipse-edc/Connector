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

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":common:util"))
    api(project(":core:base"))
    api(project(":extensions:inline-data-transfer:inline-data-transfer-core"))

    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
}


publishing {
    publications {
        create<MavenPublication>("transfer") {
            artifactId = "transfer"
            from(components["java"])
        }
    }
}
