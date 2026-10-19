/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("application")
}

dependencies {
    api(project(":dist:bom:controlplane-virtual-base-bom")) {
        // the TCK acts as data plane without authentication, as for the classic DPS connector under test
        exclude("org.eclipse.edc", "data-plane-signaling-oauth2")
        exclude("org.eclipse.edc", "data-plane-signaling-oauth2-token-exchange")
    }
    api(project(":dist:bom:controlplane-virtual-feature-sql-bom"))
    api(project(":dist:bom:controlplane-virtual-feature-nats-bom"))
    implementation(project(":extensions:common:iam:iam-mock"))
    implementation(libs.jakarta.rsApi)
    runtimeOnly(libs.parsson)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}
