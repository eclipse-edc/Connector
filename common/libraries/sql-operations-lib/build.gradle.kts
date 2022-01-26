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
    `java-test-fixtures`
    `maven-publish`
}

val jupiterVersion: String by project
val testContainersVersion: String by project
val h2Version: String by project

dependencies {
    implementation(project(":spi"))
    implementation(project(":common:libraries:sql-lib"))

    testFixturesImplementation(project(":common:libraries:sql-lib"))
    testFixturesImplementation("com.h2database:h2:${h2Version}")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
}

publishing {
    publications {
        create<MavenPublication>("sql-operations-lib") {
            artifactId = "sql-operations-lib"
            from(components["java"])
        }
    }
}