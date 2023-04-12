/*
 *  Copyright (c) 2022 Microsoft Corporation
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
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":extensions:common:azure:ms-dataverse"))
    implementation(project(":extensions:common:azure:azure-blob-core"))
    implementation(project(":core:common:util"))
    implementation(root.azure.identity)
    implementation(root.azure.resourcemanager.datafactory)
    implementation(root.azure.resourcemanager.storage)
    implementation(root.azure.resourcemanager.keyvault)
    implementation(root.azure.resourcemanager)
    implementation(root.azure.resourcemanager.authorization)

    testImplementation(project(":extensions:common:configuration:configuration-filesystem"))
    testImplementation(project(":core:data-plane:data-plane-core"))
    testImplementation(project(":extensions:common:azure:azure-resource-manager"))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-blob-core")))

    testImplementation(project(":core:common:junit"))
    testImplementation(root.awaitility)
    testImplementation(root.bouncyCastle.bcprovJdk18on)
}


