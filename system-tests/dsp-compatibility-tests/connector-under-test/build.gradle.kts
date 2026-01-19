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
    api(project(":dist:bom:controlplane-base-bom"))
    api(project(":dist:bom:dataplane-base-bom"))
    api(project(":data-protocols:dsp:dsp-2025"))
    api(project(":system-tests:protocol-tck:tck-extension"))
    runtimeOnly(libs.parsson)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
