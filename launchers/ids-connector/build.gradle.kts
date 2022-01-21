/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val jupiterVersion: String by project

dependencies {
    api(project(":core"))

    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:negotiation-store-memory"))
    implementation(project(":extensions:in-memory:contractdefinition-store-memory"))

    implementation(project(":data-protocols:ids"))

    implementation(project(":extensions:iam:iam-mock"))

    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation(project(":extensions:api:control"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}
