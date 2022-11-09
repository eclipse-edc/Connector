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
    implementation(project(":extensions:common:http"))
    implementation(project(":core:common:boot"))
    implementation(project(":core:common:connector-core"))

    implementation(libs.nimbus.jwt)
    implementation(libs.okhttp)
    implementation(libs.jakarta.rsApi)
}
