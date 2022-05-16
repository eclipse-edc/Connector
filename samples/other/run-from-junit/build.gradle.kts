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

val jupiterVersion: String by project
val storageBlobVersion: String by project;

dependencies {
    api(project(":core"))
    api(project(":core:defaults"))

    testImplementation(project(":extensions:aws:s3:s3-provision"))
    testImplementation(project(":data-protocols:ids:ids-core"))
    testImplementation(project(":extensions:filesystem:configuration-fs"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation(testFixtures(project(":launchers:junit")))


}
