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

val awsVersion: String by project

dependencies {
    api(project(":spi"))

    api(project(":extensions:aws:s3:s3-provision"))
    api(project(":extensions:aws:s3:s3-core"))
}

publishing {
    publications {
        create<MavenPublication>("aws-s3") {
            artifactId = "aws-s3"
            from(components["java"])
        }
    }
}
