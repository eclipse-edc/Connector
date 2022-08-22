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
    implementation(project(":common:token-generation-lib"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":data-protocols:ids"))
    implementation(project(":extensions:filesystem:vault-fs"))
    implementation(project(":extensions:http"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":extensions:api:data-management"))
    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-sync"))

    implementation(project(":core:data-plane-selector:data-plane-selector-core"))
    implementation(project(":extensions:data-plane-selector:selector-api"))
    implementation(project(":extensions:data-plane-selector:selector-client"))

    implementation(project(":extensions:http-provisioner"))
    implementation(project(":extensions:http-receiver"))
}
