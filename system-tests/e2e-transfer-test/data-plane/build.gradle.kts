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
    api(project(":spi:web-spi"))
    api(project(":core:base"))
    api(project(":core:boot"))
    api(project(":extensions:http"))

    api(project(":extensions:data-plane:data-plane-spi"))
    api(project(":extensions:data-plane:data-plane-framework"))
    api(project(":extensions:data-plane:data-plane-http"))
    api(project(":extensions:data-plane:data-plane-api"))
}
