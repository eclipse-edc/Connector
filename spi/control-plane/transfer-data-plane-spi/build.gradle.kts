/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi:common:core-spi"))
}

publishing {
    publications {
        create<MavenPublication>("transfer-data-plane-spi") {
            artifactId = "transfer-data-plane-spi"
            from(components["java"])
        }
    }
}
