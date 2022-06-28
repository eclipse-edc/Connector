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
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val jupiterVersion: String by project

dependencies {
    api(project(":core:boot"))
    implementation(project(":data-protocols:ids")) {
        exclude("org.eclipse.dataspaceconnector","ids-token-validation")
    }

    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")

}

application {
    mainClass.set("org.eclipse.dataspaceconnector.demo.runtime.CustomRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("custom-runtime.jar")
}
