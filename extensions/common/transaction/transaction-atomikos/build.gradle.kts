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
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:transaction-datasource-spi"))

    implementation(project(":core:common:lib:util-lib"))
    implementation(libs.jakarta.transaction.api)
    implementation(libs.atomikos.jta) { artifact { classifier = "jakarta" } }
    implementation(libs.atomikos.jdbc)

    testImplementation(libs.h2)
}


