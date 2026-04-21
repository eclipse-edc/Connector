/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
}

dependencies {
    api(project(":dist:bom:controlplane-virtual-base-bom")) {
        exclude("org.eclipse.edc", "data-plane-signaling")
        exclude("org.eclipse.edc", "data-plane-signaling-oauth2")
    }
    api(project(":dist:bom:controlplane-virtual-feature-sql-bom"))
    api(project(":dist:bom:controlplane-virtual-feature-nats-bom"))
    api(project(":system-tests:tck:tasks-tck-extension"))
    api(project(":extensions:common:iam:iam-mock"))
    runtimeOnly(libs.parsson)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}