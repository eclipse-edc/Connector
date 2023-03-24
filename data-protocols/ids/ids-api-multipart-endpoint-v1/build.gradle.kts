/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - added dependency
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:ids:ids-spi"))
    api(project(":data-protocols:ids:ids-core"))
    api(project(":data-protocols:ids:ids-transform-v1"))
    api(project(":extensions:common:http"))

    implementation(project(":data-protocols:ids:ids-api-configuration"))

    implementation(libs.jakarta.rsApi)
    implementation(libs.jersey.multipart)

    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.37.0")
    testImplementation("net.javacrumbs.json-unit:json-unit-json-path:2.37.0")
    testImplementation("net.javacrumbs.json-unit:json-unit:2.37.0")

    testImplementation(project(":core:common:junit"))

    testImplementation(project(":core:control-plane:control-plane-core"))

}


