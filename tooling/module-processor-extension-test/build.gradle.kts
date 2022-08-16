/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":spi:common:core-spi"))
    implementation(project(":tooling:module-domain"))
    annotationProcessor(project(":tooling:module-processor"))

}

tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Aedc.version=${project.version}")
    compilerArgs.add("-Aedc.id=${project.group}:${project.name}")
    compilerArgs.add("-Aedc.location=${project.projectDir.absolutePath}/build")
    outputs.upToDateWhen { false }
}