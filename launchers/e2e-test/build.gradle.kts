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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    api(project(":core"))
    api(project(":data-protocols:ids"))

    api(project(":extensions:http"))
    api(project(":extensions:api:control"))
    api(project(":extensions:api:observability"))

    api(project(":extensions:filesystem:configuration-fs"))

    api(project(":extensions:in-memory:transfer-store-memory"))
    api(project(":extensions:in-memory:assetindex-memory"))
    api(project(":extensions:in-memory:negotiation-store-memory"))
    api(project(":extensions:in-memory:contractdefinition-store-memory"))

    api(project(":extensions:iam:iam-mock"))

    testImplementation(testFixtures(project(":common:util")))
    testImplementation("io.rest-assured:rest-assured:4.4.0")
    testImplementation("org.awaitility:awaitility:4.1.1")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("failed", "passed", "skipped")
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
