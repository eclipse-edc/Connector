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
    implementation(project(":dist:bom:controlplane-base-bom"))
    implementation(project(":extensions:common:iam:iam-mock"))

    implementation(project(":extensions:common:metrics:micrometer-core"))
    implementation(project(":extensions:common:http:jersey-micrometer"))
    implementation(project(":extensions:common:http:jetty-micrometer"))
}

edcBuild {
    publish.set(false)
}
