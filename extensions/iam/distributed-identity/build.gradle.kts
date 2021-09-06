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
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
    api(project(":extensions:iam:distributed-identity:identity-did-core"))
    api(project(":extensions:iam:distributed-identity:identity-did-core"))
}

publishing {
    publications {
        create<MavenPublication>("iam.distributed") {
            artifactId = "iam.distributed"
            from(components["java"])
        }
    }
}
