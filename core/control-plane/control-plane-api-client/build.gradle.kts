/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
    `java-library`
}

val failsafeVersion: String by project
val okHttpVersion: String by project
val awaitility: String by project



dependencies {
    api(project(":spi:control-plane:control-plane-api-client-spi"))

    implementation("dev.failsafe:failsafe:${failsafeVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:data-plane:data-plane-core"))
    testImplementation(project(":core:control-plane:control-plane-api"))
    testImplementation(project(":extensions:common:auth:auth-tokenbased"))
    testImplementation("org.awaitility:awaitility:${awaitility}")

}

publishing {
    publications {
        create<MavenPublication>("control-plane-api-client") {
            artifactId = "control-plane-api-client"
            from(components["java"])
        }
    }
}
