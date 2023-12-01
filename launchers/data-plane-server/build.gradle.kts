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
 *       Mercedes-Benz Tech Innovation GmbH - publish public api context into dedicated swagger hub page
 *
 */


plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":spi:common:web-spi"))
    implementation(project(":extensions:common:metrics:micrometer-core"))
    implementation(project(":core:data-plane:data-plane-core"))
    implementation(project(":extensions:common:http"))
    implementation(project(":extensions:common:configuration:configuration-filesystem"))
    implementation(project(":extensions:data-plane:data-plane-http"))
    implementation(project(":extensions:data-plane:data-plane-control-api"))
    implementation(project(":extensions:data-plane:data-plane-public-api"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm", "jndi.properties", "jetty-dir.css", "META-INF/maven/**")
    mergeServiceFiles()
    archiveFileName.set("data-plane-server.jar")
}

edcBuild {
    publish.set(false)
}
