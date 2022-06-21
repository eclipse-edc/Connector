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

val jettyVersion: String by project
val micrometerVersion: String by project

dependencies {
    implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${jettyVersion}")
    implementation(project(":extensions:http:jetty"))
    api("io.micrometer:micrometer-core:${micrometerVersion}")

    api(project(":spi:core-spi"))

}

publishing {
    publications {
        create<MavenPublication>("jetty-micrometer") {
            artifactId = "jetty-micrometer"
            from(components["java"])
        }
    }
}
