/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Catena-X Consortium - initial API and implementation
 *
 */

plugins {
    `java-library`
}

val atomikosVersion: String by project
val h2Version: String by project
val jtaVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:transaction-spi"))
    api(project(":extensions:transaction:transaction-datasource-spi"))

    implementation(project(":common:util"))
    implementation("javax.transaction:javax.transaction-api:${jtaVersion}")
    implementation("com.atomikos:transactions-jta:${atomikosVersion}")
    implementation("com.atomikos:transactions-jdbc:${atomikosVersion}")

    testImplementation("com.h2database:h2:${h2Version}")
}

publishing {
    publications {
        create<MavenPublication>("transaction-atomikos") {
            artifactId = "transaction-atomikos"
            from(components["java"])
        }
    }
}
