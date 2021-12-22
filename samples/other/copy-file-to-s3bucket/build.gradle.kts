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

val awsVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    implementation(project(":extensions:aws:s3:writer"))
    implementation(project(":extensions:inline-data-transfer:inline-data-transfer-core"))


    implementation(platform("software.amazon.awssdk:bom:${awsVersion}"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:auth")
    implementation("software.amazon.awssdk:iam")
}


