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
}

dependencies {
    implementation(project(":core:common:token-core"))
    implementation(project(":core:common:edr-store-core"))
    implementation(project(":core:common:connector-core"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":data-protocols:dsp"))
    implementation(project(":extensions:common:api:api-observability"))
    implementation(project(":extensions:common:http"))
    implementation(project(":extensions:common:iam:iam-mock"))
    implementation(project(":extensions:common:json-ld"))
    implementation(project(":extensions:common:api:control-api-configuration"))
    implementation(project(":extensions:control-plane:api:management-api"))
    implementation(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-client"))

    implementation(project(":core:data-plane-selector:data-plane-selector-core"))

    implementation(project(":extensions:common:metrics:micrometer-core"))
    implementation(project(":extensions:common:http:jersey-micrometer"))
    implementation(project(":extensions:common:http:jetty-micrometer"))
}

edcBuild {
    publish.set(false)
}
