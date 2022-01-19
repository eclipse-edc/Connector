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
    api(project(":core"))

    api("software.amazon.awssdk:sts:${awsVersion}")
    api("software.amazon.awssdk:iam:${awsVersion}")
    api("software.amazon.awssdk:s3:${awsVersion}")
}

publishing {
    publications {
        create<MavenPublication>("s3-core") {
            artifactId = "s3-core"
            from(components["java"])
        }
    }
}
