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
 *       Daimler TSS GmbH - Initial build file
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val apacheCommonsPool2Version: String by project
val assertj: String by project
val mockitoVersion: String by project
val postgresVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":extensions:common:sql:sql-core"))

    testImplementation(project(":extensions:common:junit"))
    testImplementation(project(":extensions:common:transaction:transaction-local"))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-lease")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))
    testImplementation(testFixtures(project(":core:common:util")))
    testImplementation("org.postgresql:postgresql:${postgresVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
}

publishing {
    publications {
        create<MavenPublication>("sql-lease") {
            artifactId = "sql-lease"
            from(components["java"])
        }
    }
}