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
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val jupiterVersion: String by project
val rsApi: String by project

dependencies {
    implementation(project(":core"))
    implementation(project(":core:micrometer"))

    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:negotiation-store-memory"))
    implementation(project(":extensions:in-memory:contractdefinition-store-memory"))

    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation(project(":extensions:iam:iam-mock"))

    implementation(project(":extensions:api:control"))

    implementation(project(":data-protocols:ids"))
    runtimeOnly(project(":extensions:http:jersey-micrometer"))
    runtimeOnly(project(":extensions:http:jetty-micrometer"))
    implementation(project(":samples:04.0-file-transfer:api"))
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}
