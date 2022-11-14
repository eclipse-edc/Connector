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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

plugins {
    `java-library`
}

dependencies {
    testImplementation(libs.gatling) {
        exclude(group = "io.gatling", module = "gatling-jms")
        exclude(group = "io.gatling", module = "gatling-jms-java")
        exclude(group = "io.gatling", module = "gatling-mqtt")
        exclude(group = "io.gatling", module = "gatling-mqtt-java")
        exclude(group = "io.gatling", module = "gatling-jdbc")
        exclude(group = "io.gatling", module = "gatling-jdbc-java")
        exclude(group = "io.gatling", module = "gatling-redis")
        exclude(group = "io.gatling", module = "gatling-redis-java")
        exclude(group = "io.gatling", module = "gatling-graphite")
    }

    testImplementation(project(":extensions:common:azure:azure-blob-core"))
    testImplementation(project(":extensions:common:vault:vault-azure"))
    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":system-tests:tests")))
    testImplementation(testFixtures(project(":system-tests:azure-tests")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(libs.azure.storageblob)
    testImplementation(libs.restAssured)
    testImplementation(libs.azure.identity)
    testImplementation(libs.azure.keyvault)

    testRuntimeOnly(project(":system-tests:runtimes:azure-data-factory-transfer-provider"))
    testRuntimeOnly(project(":system-tests:runtimes:azure-data-factory-transfer-consumer"))
}
