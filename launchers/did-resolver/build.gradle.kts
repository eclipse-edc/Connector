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
    implementation(project(":common:util"))
    implementation(project(":core:protocol:web"))

//    implementation(project(":core:transfer"))
    implementation(project(":extensions:azure:events"))
//
//    implementation(project(":data-protocols:ids"))
//
//    implementation(project(":extensions:azure:vault"))
//    implementation(project(":extensions:in-memory:policy-registry-memory"))
//    implementation(project(":data-protocols:ids:ids-policy-mock"))
//    implementation(project(":extensions:filesystem:configuration-fs"))

    implementation(project(":samples:gaiax-hackathon-1:ion"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}

application {
    @Suppress("DEPRECATION")
    mainClassName = "org.eclipse.dataspaceconnector.did.ServiceRegistrationRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("did-resolver.jar")
}
