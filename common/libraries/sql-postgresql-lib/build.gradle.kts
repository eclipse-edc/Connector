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

val postgresqlVersion: String by project
val mockitoVersion: String by project

dependencies {
    api(project(":common:libraries:sql-lib"))

    implementation("org.postgresql:postgresql:${postgresqlVersion}")

    testImplementation(testFixtures(project(":common:util")))

    // required for statically mocking the JDBC DriverManager
    testImplementation("org.mockito:mockito-inline:${mockitoVersion}")
}

publishing {
    publications {
        create<MavenPublication>("common-sql-postgresql-lib") {
            artifactId = "common-sql-postgresql-lib"
            from(components["java"])
        }
    }
}
