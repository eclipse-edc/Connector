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
}

val micrometerVersion: String by project

dependencies {
    implementation(project(":extensions:http:jersey"))
    implementation("io.micrometer:micrometer-core:${micrometerVersion}")
}

publishing {
    publications {
        create<MavenPublication>("jersey-micrometer") {
            artifactId = "jersey-micrometer"
            from(components["java"])
        }
    }
}
