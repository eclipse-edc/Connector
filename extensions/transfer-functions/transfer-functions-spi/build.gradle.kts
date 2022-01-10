/*
 *  Copyright (c) 2021 Microsoft Corporation
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

val okHttpVersion: String by project


dependencies {
    api(project(":spi"))
    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")

}

publishing {
    publications {
        create<MavenPublication>("transfer-functions-spi") {
            artifactId = "transfer-functions-spi"
            from(components["java"])
        }
    }
}
