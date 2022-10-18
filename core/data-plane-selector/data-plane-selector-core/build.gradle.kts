/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

val mockitoVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    api(project(":core:common:connector-core"))
    api(project(":core:common:boot"))
    implementation(project(":core:common:util"))

    testImplementation(testFixtures(project(":spi:data-plane-selector:data-plane-selector-spi")))

}

publishing {
    publications {
        create<MavenPublication>("data-plane-selector-core") {
            artifactId = "data-plane-selector-core"
            from(components["java"])
        }
    }
}
