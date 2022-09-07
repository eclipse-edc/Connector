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
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val rsApi: String by project

dependencies {
    implementation(project(":core:control-plane:control-plane-core"))

    implementation(project(":extensions:common:api:observability"))

    implementation(project(":extensions:common:configuration:filesystem-configuration"))
    implementation(project(":extensions:common:iam:iam-mock"))

    implementation(project(":extensions:control-plane:api:data-management"))

    implementation(project(":data-protocols:ids"))

    implementation(project(":extensions:control-plane:provision:blob-provision"))
    implementation(project(":extensions:common:vault:azure-vault"))
    implementation(project(":core:common:util"))

    api("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}
