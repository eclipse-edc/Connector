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

val okHttpVersion: String by project
val rsApi: String by project

dependencies {
    implementation(project(":extensions:http"))
    implementation(project(":core:boot"))
    implementation(project(":core:base"))

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:$rsApi")
}
