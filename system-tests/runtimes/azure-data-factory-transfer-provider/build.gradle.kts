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
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val rsApi: String by project
val azureIdentityVersion: String by project
val azureResourceManagerDataFactory: String by project
val azureResourceManagerVersion: String by project


dependencies {
    implementation(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane:data-plane-core"))
    implementation(project(":extensions:control-plane:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-selector:data-plane-selector-client"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))
    implementation(project(":extensions:data-plane:data-plane-azure-data-factory"))
    implementation(project(":extensions:common:azure:azure-resource-manager"))
    implementation(project(":extensions:common:api:api-observability"))
    implementation(project(":extensions:common:configuration:configuration-filesystem"))
    implementation(project(":extensions:common:iam:iam-mock"))
    implementation(project(":extensions:control-plane:api:data-management-api"))
    implementation(project(":extensions:control-plane:provision:provision-blob"))
    implementation(project(":extensions:common:vault:vault-azure"))
    implementation(project(":data-protocols:ids"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("com.azure:azure-identity:${azureIdentityVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-datafactory:${azureResourceManagerDataFactory}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-storage:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-keyvault:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-authorization:${azureResourceManagerVersion}")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}