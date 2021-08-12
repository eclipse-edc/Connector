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
    implementation(project(":core:bootstrap"))
    implementation(project(":common:util"))
    implementation(project(":core:protocol:web"))
    implementation(project(":extensions:azure:events-config"))
    implementation(project(":samples:gaiax-hackathon-1:identity:ion-core"))
    implementation(project(":samples:gaiax-hackathon-1:identity:registration-service"))
    implementation(project(":samples:gaiax-hackathon-1:identity:registration-service-api"))
    implementation(project(":samples:gaiax-hackathon-1:identity:did-document-store-inmem"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}

application {
    @Suppress("DEPRECATION")
    mainClassName = "org.eclipse.dataspaceconnector.did.RegistrationServiceRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("reg-svc.jar")
}
