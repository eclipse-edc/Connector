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
val jupiterVersion: String by project
val assertj: String by project

plugins {
    `java-library`
    `java-test-fixtures`
}
dependencies {
    api(project(":spi:common:core-spi"))
    implementation(project(":core:common:util"))


    // needed by the abstract test spec located in testFixtures
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
    testFixturesImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testFixturesImplementation("org.assertj:assertj-core:${assertj}")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}


publishing {
    publications {
        create<MavenPublication>("data-plane-selector-spi") {
            artifactId = "data-plane-selector-spi"
            from(components["java"])
        }
    }
}
