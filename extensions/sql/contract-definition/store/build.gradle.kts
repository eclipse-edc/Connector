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

dependencies {
    implementation(project(":spi:core-spi"))
    implementation(project(":spi:contract-spi"))
    implementation(project(":extensions:dataloading"))
    implementation(project(":extensions:transaction:transaction-spi"))
    implementation(project(":extensions:transaction:transaction-datasource-spi"))
    implementation(project(":extensions:sql:common"))

    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(project(":core:base"))
    testImplementation(project(":extensions:sql:pool:apache-commons-pool"))
    testImplementation(project(":extensions:transaction:transaction-local"))
    testImplementation("com.h2database:h2:${h2Version}")
    testImplementation("org.assertj:assertj-core:${assertj}")
}

publishing {
    publications {
        create<MavenPublication>("sql-contractdefinition-store") {
            artifactId = "sql-contractdefinition-store"
            from(components["java"])
        }
    }
}
