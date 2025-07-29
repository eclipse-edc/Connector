/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
}

dependencies {
    implementation(project(":dist:bom:controlplane-base-bom"))
    implementation(project(":core:common:edr-store-core"))
    implementation(project(":core:common:token-core"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":data-protocols:dsp"))
    implementation(project(":data-protocols:dsp:dsp-2025"))
    implementation(project(":extensions:common:http"))
    implementation(project(":extensions:common:api:control-api-configuration"))
    implementation(project(":extensions:common:api:management-api-configuration"))
    implementation(project(":extensions:common:iam:iam-mock"))
}

edcBuild {
    publish.set(false)
}
