/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft - initial API and implementation
 *
 */


plugins {
    `java-library`
}

dependencies {
    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))
    implementation(project(":core:common:lib:policy-evaluator-lib"))

    testImplementation(project(":spi:control-plane-spi"))
    testImplementation(project(":core:common:junit-base"))
}


