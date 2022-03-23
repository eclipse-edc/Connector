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
}

val jupiterVersion: String by project

dependencies {
    api(project(":core:boot"))
    api(project(":extensions:dataloading"))

    // the following lines enable the CosmosDB-based AssetIndex
    implementation(project(":extensions:azure:cosmos:assetindex-cosmos"))
    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation(project(":extensions:azure:vault"))

    // alternatively uncomment the following line to use the in-memory AssetIndex
    //
    // implementation(project(":extensions:in-memory:assetindex-memory"))

    // the following line enables the Cosmos-DB based ContractDefinitionStore
    implementation(project(":extensions:azure:cosmos:contract-definition-store-cosmos"))


    // lightweight lib for CLI args
    api("info.picocli:picocli:4.6.2")

    testImplementation(testFixtures(project(":common:util")))
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.dataloader.cli.DataLoaderRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataloader.jar")
}

publishing {
    publications {
        create<MavenPublication>("dataloader-cli") {
            artifactId = "dataloader-cli"
            from(components["java"])
        }
    }
}

