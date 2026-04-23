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
}

dependencies {
    implementation(project(":core:common:token-core"))
    implementation(project(":core:common:runtime-core"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:control-plane:control-plane-contract-task-executor"))
    implementation(project(":core:control-plane:control-plane-transfer-task-executor"))
    implementation(project(":core:common:participant-context-connector-core"))
    implementation(project(":core:common:task-core"))
    implementation(project(":data-protocols:dsp:dsp-virtual"))
    implementation(project(":extensions:common:http"))
    implementation(project(":extensions:common:json-ld"))
    implementation(project(":extensions:common:api:control-api-configuration"))
    implementation(project(":extensions:common:api:management-api-schema-validator"))

    implementation(project(":extensions:control-plane:api:management-api-v5"))
    implementation(project(":extensions:common:api:management-api-authorization"))
    implementation(project(":extensions:common:api:management-api-oauth2-authentication"))
    implementation(project(":core:common:cel-core"))

    implementation(project(":data-protocols:data-plane-signaling:data-plane-signaling-core"))
    implementation(project(":data-protocols:data-plane-signaling:data-plane-signaling-oauth2"))
    implementation(project(":extensions:common:iam:oauth2:oauth2-client"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))
}

edcBuild {
    publish.set(false)
}
