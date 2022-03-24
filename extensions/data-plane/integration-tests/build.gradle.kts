/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    java
}

val jupiterVersion: String by project
val restAssured: String by project
val assertj: String by project
val awaitility: String by project
val faker: String by project
val httpMockServer: String by project

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testImplementation("org.mock-server:mockserver-netty:${httpMockServer}:shaded")
    testImplementation("org.mock-server:mockserver-client-java:${httpMockServer}:shaded")

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-http")))
    testImplementation(project(":extensions:data-plane:data-plane-spi"))

    testRuntimeOnly(project(":launchers:data-plane-server"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
