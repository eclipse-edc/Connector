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
    implementation(project(":core:protocol:web"))
    implementation(project(":core:transfer"))
    implementation(project(":core:bootstrap"))
    implementation(project(":core:policy:policy-model"))
    implementation(project(":core:policy:policy-engine"))
    implementation(project(":core:schema"))



    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:azure:vault"))
    implementation(project(":extensions:in-memory:policy-registry-memory"))
    implementation(project(":extensions:in-memory:metadata-memory"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    implementation(project(":data-protocols:ids"))
    implementation(project(":data-protocols:ids:ids-policy-mock"))

    implementation(project(":samples:gaiax-hackathon-1:identity:cloud-transfer"))

    implementation(project(":samples:gaiax-hackathon-1:identity:control-rest"))
    implementation(project(":samples:gaiax-hackathon-1:identity:did-service"))
    implementation(project(":samples:gaiax-hackathon-1:identity:identity-hub-spi"))
    implementation(project(":samples:gaiax-hackathon-1:identity:identity-hub-core"))
    implementation(project(":samples:gaiax-hackathon-1:identity:identity-hub-memory"))


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
