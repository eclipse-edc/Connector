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

val infoModelVersion: String by project
val jlineVersion: String by project
val okHttpVersion: String by project
val jacksonVersion: String by project

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation(project(":samples:other:commandline:consumer-runtime"))

    implementation(project(":extensions:iam:oauth2"))  // required for now
    implementation(project(":extensions:filesystem:vault-fs"))  // required for now

    implementation("org.jline:jline:${jlineVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")
}

// workaround for this issue: https://github.com/johnrengelman/shadow/issues/609
application {
    mainClass.set("org.eclipse.dataspaceconnector.client.Main")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspaceconnector-client.jar")
}

