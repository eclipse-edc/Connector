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
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val rsApi: String by project
val azureIdentityVersion: String by project
val azureResourceManagerDataFactory: String by project
val azureResourceManagerVersion: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))

    implementation(project(":core:transfer"))
    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-selector:selector-client"))
    implementation(project(":extensions:data-plane-selector:selector-core"))
    implementation(project(":extensions:data-plane-selector:selector-store"))
    implementation(project(":extensions:data-plane:data-plane-framework"))
    implementation(project(":extensions:azure:data-plane:data-factory"))
    implementation(project(":extensions:azure:resource-manager"))
    implementation("com.azure:azure-identity:${azureIdentityVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-datafactory:${azureResourceManagerDataFactory}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-storage:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-keyvault:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager:${azureResourceManagerVersion}")
    implementation("com.azure.resourcemanager:azure-resourcemanager-authorization:${azureResourceManagerVersion}")

    implementation(project(":extensions:data-plane:data-plane-spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    implementation(project(":core"))

    implementation(project(":extensions:api:observability"))

    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":extensions:api:data-management"))
    implementation(project(":extensions:azure:blobstorage"))
    implementation(project(":extensions:azure:vault"))

    implementation(project(":data-protocols:ids")) {
        exclude("org.eclipse.dataspaceconnector","ids-token-validation")
    }
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}
