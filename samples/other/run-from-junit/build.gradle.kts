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

val storageBlobVersion: String by project;
val jupiterVersion: String by project

dependencies {
    api(project(":core:bootstrap"))
    api(project(":core:transfer"))
    api(project(":extensions:in-memory:transfer-store-memory"))

    api(project(":extensions:aws:s3:provision"))

    api(project(":data-protocols:ids:ids-core"))

    testImplementation(project(":extensions:filesystem:configuration-fs"))
    testImplementation(project(":extensions:azure:vault"))
    testImplementation("com.azure:azure-storage-blob:${storageBlobVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation(testFixtures(project(":launchers:junit")))


}
