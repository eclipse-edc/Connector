/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}


dependencies {
    implementation(project(":core:protocol:web"))

    implementation(project(":core:transfer"))
    implementation(project(":extensions:azure:transfer-process-store-cosmos"))
    implementation(project(":extensions:aws:s3:provision"))
    implementation(project(":extensions:azure:blob:provision"))
    implementation(project(":samples:copy-with-nifi:transfer"))

    implementation(project(":extensions:azure:events"))

    implementation(project(":data-protocols:ids"))

    implementation(project(":extensions:atlas"))
    implementation(project(":samples:dataseed"))

    implementation(project(":extensions:azure:vault"))
    implementation(project(":extensions:in-memory:policy-registry-memory"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":data-protocols:ids:ids-policy-mock"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}

application {
    @Suppress("DEPRECATION")
    mainClassName = "org.eclipse.edc.runtime.EdcRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("edc-demo-e2e.jar")
}
