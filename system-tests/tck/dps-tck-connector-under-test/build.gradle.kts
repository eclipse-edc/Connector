/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("application")
}

dependencies {
    implementation(project(":dist:bom:controlplane-base-bom"))
    implementation(project(":data-protocols:data-plane-signaling:data-plane-signaling-core"))
    implementation(project(":spi:common:participant-context-single-spi"))
    implementation(project(":spi:common:web-spi"))
    implementation(project(":spi:control-plane:control-plane-spi"))
    implementation(project(":spi:control-plane:transfer-spi"))
    implementation(project(":spi:data-plane-selector:data-plane-selector-spi"))
    implementation(project(":extensions:common:iam:iam-mock"))
    implementation(libs.jakarta.rsApi)
    runtimeOnly(libs.parsson)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
