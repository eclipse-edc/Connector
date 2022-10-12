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

val assertj: String by project
val postgresVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":extensions:common:sql:sql-core"))


    testImplementation(project(":extensions:common:junit"))
    testImplementation("org.postgresql:postgresql:${postgresVersion}")
    testImplementation(testFixtures(project(":core:common:util")))
    testImplementation(testFixtures(project(":spi:control-plane:policy-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))
}

publishing {
    publications {
        create<MavenPublication>("policy-definition-store-sql") {
            artifactId = "policy-definition-store-sql"
            from(components["java"])
        }
    }
}