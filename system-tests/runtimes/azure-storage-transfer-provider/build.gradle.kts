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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val rsApi: String by project

dependencies {
    implementation(project(":common:util"))

    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane:data-plane-core"))

    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-selector:selector-client"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))
    implementation(project(":extensions:azure:data-plane:storage"))

    implementation(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":extensions:api:observability"))

    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":extensions:api:data-management"))

    implementation(project(":data-protocols:ids"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}
