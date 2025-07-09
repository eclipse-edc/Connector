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
 *       Mercedes-Benz Tech Innovation GmbH - publish public api context into dedicated swagger hub page
 */

plugins {
    `java-library`
}

dependencies {
    implementation(project(":dist:bom:dataplane-base-bom"))

    implementation(project(":extensions:data-plane:data-plane-kafka"))
}

edcBuild {
    publish.set(false)
}
