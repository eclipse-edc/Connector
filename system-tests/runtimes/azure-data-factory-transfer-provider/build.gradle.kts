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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane:data-plane-core"))
    implementation(project(":extensions:control-plane:transfer:transfer-data-plane"))
    implementation(project(":extensions:data-plane:data-plane-client"))
    implementation(project(":extensions:data-plane-selector:data-plane-selector-client"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))
    implementation(project(":extensions:data-plane:data-plane-azure-data-factory"))
    implementation(project(":extensions:common:azure:azure-resource-manager"))
    implementation(project(":extensions:common:api:api-observability"))
    implementation(project(":extensions:common:configuration:configuration-filesystem"))
    implementation(project(":extensions:common:iam:iam-mock"))
    implementation(project(":extensions:control-plane:api:management-api"))
    implementation(project(":extensions:control-plane:provision:provision-blob"))
    implementation(project(":extensions:common:vault:vault-azure"))
    implementation(project(":data-protocols:ids"))

    implementation(root.jakarta.rsApi)
    implementation(root.azure.identity)
    implementation(root.azure.resourcemanager.datafactory)
    implementation(root.azure.resourcemanager.storage)
    implementation(root.azure.resourcemanager.keyvault)
    implementation(root.azure.resourcemanager)
    implementation(root.azure.resourcemanager.authorization)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}

edcBuild {
    publish.set(false)
}
