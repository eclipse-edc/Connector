/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}


dependencies {
    implementation(project(":spi"))
    implementation(project(":core"))
    implementation(project(":core:transfer"))
    implementation(project(":common:util"))

    implementation(project(":extensions:azure:vault"))
    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))

    implementation(project(":samples:gaiax-hackathon-1:identity:transfer"))


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}

application {
    @Suppress("DEPRECATION")
    mainClassName = "org.eclipse.dataspaceconnector.did.ProviderRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}
