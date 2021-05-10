/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
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
    implementation(project(":client-runtime"))

    implementation(project(":extensions:iam:oauth2"))  // required for now
    implementation(project(":extensions:security:security-fs"))  // required for now

    implementation("org.jline:jline:${jlineVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")
}

// workaround for this issue: https://github.com/johnrengelman/shadow/issues/609
application {
    @Suppress("DEPRECATION")
    mainClassName = "com.microsoft.dagx.client.Main"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dagx-client.jar")
}

