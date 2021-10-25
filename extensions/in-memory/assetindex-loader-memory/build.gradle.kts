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
 *
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:dataaddress-resolver-memory"))
}
publishing {
    publications {
        create<MavenPublication>("in-memory.assetindex-loader") {
            artifactId = "in-memory.assetindex-loader"
            from(components["java"])
        }
    }
}
