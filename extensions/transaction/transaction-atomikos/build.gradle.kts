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


dependencies {
    api(project(":spi:core-spi"))
    api(project(":extensions:transaction:transaction-spi"))
    api(project(":extensions:transaction:transaction-datasource-spi"))

    implementation("javax.transaction:javax.transaction-api:1.3")
    implementation("com.atomikos:transactions-jta:5.0.8")
    implementation("com.atomikos:transactions-jdbc:5.0.8")

    testImplementation("com.h2database:h2:2.1.210")
}

publishing {
    publications {
        create<MavenPublication>("transaction-atomikos") {
            artifactId = "transaction-atomikos"
            from(components["java"])
        }
    }
}
