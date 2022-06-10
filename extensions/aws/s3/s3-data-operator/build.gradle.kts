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
    api(project(":core"))
    api(project(":extensions:aws:s3:s3-core"))


    testImplementation(testFixtures(project(":extensions:aws:aws-test")))
}

publishing {
    publications {
        create<MavenPublication>("s3-operator") {
            artifactId = "s3-operator"
            from(components["java"])
        }
    }
}
