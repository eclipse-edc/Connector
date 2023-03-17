/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

dependencies {
    implementation(project(":core:control-plane:control-plane-core"))

    implementation(project(":data-protocols:dsp:dsp-api-configuration"))
    //implementation(project(":data-protocols:dsp:dsp-catalog"))
    implementation(project(":data-protocols:dsp:dsp-control-plane"))
    implementation(project(":data-protocols:dsp:dsp-core"))
    implementation(project(":data-protocols:dsp:dsp-spi:dsp-catalog-spi"))
    implementation(project(":data-protocols:dsp:dsp-spi:dsp-control-plane-spi"))
    implementation(project(":data-protocols:dsp:dsp-spi:dsp-core-spi"))

    implementation(project(":extensions:common:configuration:configuration-filesystem"))
    implementation(project(":extensions:common:http"))

    implementation(project(":extensions:common:iam:iam-mock"))

    implementation(project(":extensions:control-plane:api:management-api"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}

edcBuild {
    publish.set(false)
}
