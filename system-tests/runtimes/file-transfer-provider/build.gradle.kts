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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

dependencies {
    implementation(project(":core:common:util"))

    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane:data-plane-core"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation(project(":extensions:control-plane:transfer:transfer-data-plane"))
    implementation(project(":extensions:data-plane:data-plane-client"))
    implementation(project(":extensions:data-plane-selector:data-plane-selector-client"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))

    implementation(libs.jakarta.rsApi)
    implementation(libs.opentelemetry.annotations)

    implementation(project(":extensions:common:api:api-observability"))

    implementation(project(":extensions:common:configuration:configuration-filesystem"))
    implementation(project(":extensions:common:iam:iam-mock"))

    implementation(project(":data-protocols:ids"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}

edcBuild {
    publish.set(false)
}
