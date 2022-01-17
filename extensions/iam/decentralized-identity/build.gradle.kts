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
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
    api(project(":extensions:iam:decentralized-identity:identity-did-core"))
    api(project(":extensions:iam:decentralized-identity:identity-did-service"))
//    api(project(":extensions:iam:decentralized-identity:registration-service"))
//    api(project(":extensions:iam:decentralized-identity:registration-service-api"))
    api(project(":extensions:iam:decentralized-identity:identity-common-test"))
}

publishing {
    publications {
        create<MavenPublication>("decentralized-identity") {
            artifactId = "decentralized-identity"
            from(components["java"])
        }
    }
}
