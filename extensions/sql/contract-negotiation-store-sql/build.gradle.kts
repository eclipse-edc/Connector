/*
 *  Copyright (c) 2022 Daimler TSS GmbH
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
    `maven-publish`
}

val h2Version: String by project
val assertj: String by project
val postgresVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:contract-spi"))
    api(project(":spi:transaction-spi"))
    implementation(project(":extensions:dataloading"))
    implementation(project(":extensions:transaction:transaction-datasource-spi"))
    implementation(project(":extensions:sql:common-sql"))
    implementation(project(":extensions:sql:lease-sql"))


    testImplementation(project(":extensions:junit"))
    testImplementation(project(":core:base"))
    testImplementation(project(":core:contract"))
    testImplementation(testFixtures(project(":extensions:sql:lease-sql")))
    testImplementation("com.h2database:h2:${h2Version}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation(testFixtures(project(":common:util")))

    testImplementation("org.postgresql:postgresql:${postgresVersion}")
}

publishing {
    publications {
        create<MavenPublication>("contractnegotiation-store-sql") {
            artifactId = "contractnegotiation-store-sql"
            from(components["java"])
        }
    }
}
