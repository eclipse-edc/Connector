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
    api(project(":spi:common:web-spi"))
    api(project(":extensions:common:http:jetty-core"))

    implementation(root.bundles.jersey.core)
    implementation(libs.jetty.jakarta.servlet.api)

    testImplementation(project(":core:common:junit"))

    testImplementation(root.restAssured)
    testImplementation(root.jersey.beanvalidation) //for validation
}


