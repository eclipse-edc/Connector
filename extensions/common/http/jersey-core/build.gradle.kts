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

    implementation(libs.bundles.jersey.core)
    implementation("org.eclipse.jetty.toolchain:jetty-jakarta-servlet-api:5.0.2")

    testImplementation(project(":core:common:junit"))

    testImplementation(libs.okhttp)
    testImplementation(libs.restAssured)
    testImplementation(libs.jersey.beanvalidation) //for validation
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
