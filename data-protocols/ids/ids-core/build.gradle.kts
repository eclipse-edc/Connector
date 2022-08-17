/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - update dependencies
 *
 */

val infoModelVersion: String by project
val rsApi: String by project
val okHttpVersion: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":common:util"))
    api(project(":data-protocols:ids:ids-spi"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation(project(":data-protocols:ids:ids-jsonld-serdes-lib"))
}


publishing {
    publications {
        create<MavenPublication>("ids-core") {
            artifactId = "ids-core"
            from(components["java"])
        }
    }
}
