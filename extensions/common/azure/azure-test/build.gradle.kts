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
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:control-plane:control-plane-spi"))

    testFixturesApi(project(":core:common:util"))
    testFixturesApi(project(":core:common:junit"))
    testFixturesApi(libs.azure.cosmos)
    testFixturesApi(libs.azure.storageblob)
    testFixturesApi(libs.junit.jupiter.api)
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
