/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":spi:data-plane:data-plane-http-spi"))
    api(project(":spi:common:http-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":core:data-plane:data-plane-util"))

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:data-plane:data-plane-core"))
    testImplementation(root.restAssured)
    testImplementation(root.mockserver.netty)
}


