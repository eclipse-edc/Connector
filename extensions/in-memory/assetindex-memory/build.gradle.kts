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
}
publishing {
    publications {
        create<MavenPublication>("in-memory.assetindex") {
            artifactId = "in-memory.assetindex"
            from(components["java"])
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("in-memory.asset-index") {
            artifactId = "in-memory.asset-index"
            from(components["java"])
        }
    }
}
