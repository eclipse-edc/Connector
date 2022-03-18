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
    `java-library`
    `maven-publish`
}

val jupiterVersion: String by project
val okHttpVersion: String by project
val jodahFailsafeVersion: String by project
val micrometerVersion: String by project


dependencies {
    api(project(":core:base"))
    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    api("io.micrometer:micrometer-core:${micrometerVersion}")
}

publishing {
    publications {
        create<MavenPublication>("core-micrometer") {
            artifactId = "core-micrometer"
            from(components["java"])
        }
    }
}
