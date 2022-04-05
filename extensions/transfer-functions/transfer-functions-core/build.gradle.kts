/*
 *  Copyright (c) 2021 Microsoft Corporation
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
    api(project(":extensions:transfer-functions:transfer-functions-spi"))

    // extensions needed for integration testing
    testImplementation(project(":core:transfer"))
    testImplementation(project(":extensions:in-memory:transfer-store-memory"))
    testImplementation(project(":extensions:in-memory:policy-store-memory"))
    testImplementation(project(":extensions:in-memory:negotiation-store-memory"))
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("transfer-functions-core") {
            artifactId = "transfer-functions-core"
            from(components["java"])
        }
    }
}
