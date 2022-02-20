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
    api(project(":spi:core-spi"))
    api(project(":spi:transfer-spi"))
    implementation(project(":extensions:token:token-generation"))
}


publishing {
    publications {
        create<MavenPublication>("sync-data-transfer-consumer") {
            artifactId = "sync-data-transfer-consumer"
            from(components["java"])
        }
    }
}
