/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    implementation(project(":spi:control-plane-spi"))
    implementation(project(":spi:core-spi"))
    implementation(project(":core:common:lib:core-lib"))

    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:common:runtime-core"))
    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:common:participant-context-connector-core"))
    testImplementation(project(":core:common:participant-context-connector-classic-core"))
    testImplementation(project(":core:control-plane:control-plane-catalog"))
    testImplementation(project(":core:control-plane:control-plane-contract"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:control-plane:control-plane-contract-manager"))
    testImplementation(project(":core:control-plane:control-plane-transfer-manager"))
    testImplementation(project(":core:control-plane:control-plane-transfer"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
}


