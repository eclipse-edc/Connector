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
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}

val jupiterVersion: String by project

dependencies {
    api(project(":core:bootstrap"))

    implementation(project(":core:protocol:web"))

    implementation(project(":core:transfer"))
    implementation(project(":extensions:azure:transfer-process-store-cosmos"))
    implementation(project(":extensions:aws:s3:provision"))
    implementation(project(":extensions:azure:blob:provision"))
    implementation(project(":samples:other:copy-with-nifi:transfer"))

    implementation(project(":extensions:azure:events"))

    implementation(project(":data-protocols:ids"))

    implementation(project(":extensions:atlas"))
    implementation(project(":samples:other:dataseed"))

    implementation(project(":extensions:azure:vault"))
    implementation(project(":extensions:in-memory:policy-registry-memory"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":data-protocols:ids:ids-policy-mock"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")

}

application {
    @Suppress("DEPRECATION")
    mainClassName = "org.eclipse.dataspaceconnector.runtime.EdcRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspaceconnector-demo-e2e.jar")
}
