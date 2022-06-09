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
val mockitoVersion: String by project

dependencies {
    api(project((":spi")))
    api(project(":extensions:transaction:transaction-datasource-spi"))
    api(project(":extensions:sql:common-sql"))

    implementation("org.apache.commons:commons-pool2:${apacheCommonsPool2Version}")

    testImplementation(project(":extensions:junit"))
    testImplementation(project(":core:base"))
    testImplementation(project(":extensions:transaction:transaction-local"))

    // required for statically mocking the JDBC DriverManager
    testImplementation("org.mockito:mockito-inline:${mockitoVersion}")
}

publishing {
    publications {
        create<MavenPublication>("apache-commons-pool-sql") {
            artifactId = "apache-commons-pool-sql"
            from(components["java"])
        }
    }
}
