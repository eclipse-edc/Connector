/*
 *  Copyright (c) ZF-Group - initial API and implementation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF-Group - initial API and implementation
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
    implementation(project(":extensions:sql:common-sql"))

    testImplementation(testFixtures(project(":common:util")))
    testImplementation("com.h2database:h2:${h2Version}")
}

publishing {
    publications {
        create<MavenPublication>("policy-store-sql") {
            artifactId = "policy-store-sql"
            from(components["java"])
        }
    }
}
