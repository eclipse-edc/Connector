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


dependencies {
    api(project(":spi"))
    api(project(":extensions:inline-data-transfer:inline-data-transfer-spi"))
}

publishing {
    publications {
        create<MavenPublication>("inline-data-transfer-core") {
            artifactId = "inline-data-transfer-core"
            from(components["java"])
        }
    }
}
