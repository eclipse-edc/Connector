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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:common:transform-spi"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))

    testImplementation(project(":core:common:junit-base"));
    testImplementation(project(":core:common:lib:json-lib"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
}
