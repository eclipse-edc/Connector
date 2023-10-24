/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */


plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":core:common:boot"))
    implementation(project(":core:common:connector-core"))
    implementation(project(":core:common:jwt-core"))
    implementation(project(":extensions:common:http"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts-core"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-api"))
    api(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-client-configuration"))
    implementation(project(":extensions:common:configuration:configuration-filesystem"))
    implementation(project(":extensions:common:vault:vault-filesystem"))

}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm", "jndi.properties", "jetty-dir.css", "META-INF/maven/**")
    mergeServiceFiles()
    archiveFileName.set("sts-server.jar")
}

edcBuild {
    publish.set(false)
}
