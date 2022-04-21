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
    testImplementation(project(":core"))
    testImplementation(project(":extensions:in-memory:assetindex-memory"))
    testImplementation(project(":extensions:in-memory:contractdefinition-store-memory"))
    testImplementation(project(":extensions:in-memory:negotiation-store-memory"))
    testImplementation(project(":extensions:in-memory:policy-store-memory"))
    testImplementation(project(":extensions:in-memory:transfer-store-memory"))

    testImplementation(project(":extensions:aws:s3:s3-provision"))

    testImplementation(project(":data-protocols:ids:ids-core"))

    testImplementation(project(":extensions:filesystem:configuration-fs"))
    testImplementation(project(":extensions:azure:vault"))
    testImplementation("com.azure:azure-storage-blob:${storageBlobVersion}")
    testImplementation(project(":extensions:in-memory:policy-store-memory"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation(testFixtures(project(":launchers:junit")))


}
