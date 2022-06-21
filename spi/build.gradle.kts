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
    api(project(":spi:catalog-spi"))
    api(project(":spi:contract-spi"))
    api(project(":spi:core-spi"))
    api(project(":spi:policy-spi"))
    api(project(":spi:transaction-spi"))
    api(project(":spi:transfer-spi"))
    api(project(":spi:transport-spi"))
    api(project(":spi:web-spi"))
}

publishing {
    publications {
        create<MavenPublication>("spi") {
            artifactId = "spi"
            from(components["java"])
        }
    }
}
