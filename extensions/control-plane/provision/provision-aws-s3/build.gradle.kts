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
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":extensions:common:aws:aws-s3-core"))

    testImplementation(testFixtures(project(":extensions:common:aws:aws-s3-test")))
}

publishing {
    publications {
        create<MavenPublication>("provision-aws-s3") {
            artifactId = "provision-aws-s3"
            from(components["java"])
        }
    }
}