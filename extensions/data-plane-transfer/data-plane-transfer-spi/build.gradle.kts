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
}

publishing {
    publications {
        create<MavenPublication>("data-plane-transfer-spi") {
            artifactId = "data-plane-transfer-spi"
            from(components["java"])
        }
    }
}
