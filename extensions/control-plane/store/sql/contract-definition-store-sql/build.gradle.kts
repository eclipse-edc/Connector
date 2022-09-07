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

val assertj: String by project
val postgresVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":extensions:common:sql:common-sql"))

    testImplementation(project(":extensions:common:junit"))
    testImplementation(project(":core:common:policy-evaluator"))
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation(testFixtures(project(":core:common:util")))
    testImplementation("org.postgresql:postgresql:${postgresVersion}")
}

publishing {
    publications {
        create<MavenPublication>("contractdefinition-store-sql") {
            artifactId = "contractdefinition-store-sql"
            from(components["java"])
        }
    }
}
