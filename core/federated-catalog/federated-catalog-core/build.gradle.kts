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


dependencies {
    api(project(":core:federated-catalog:federated-catalog-cache"))
    api(project(":spi:federated-catalog:federated-catalog-spi"))
}

publishing {
    publications {
        create<MavenPublication>("federated-catalog-core") {
            artifactId = "federated-catalog-core"
            from(components["java"])
        }
    }
}
