/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    java
}

dependencies {
    testImplementation(project(":data-protocols:dsp:dsp-http-spi"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:common:json-ld-spi"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":data-protocols:dsp:dsp-08:dsp-spi-08"))
    testImplementation(project(":data-protocols:dsp:dsp-2024:dsp-spi-2024"))
    testImplementation(libs.restAssured)
}

edcBuild {
    publish.set(false)
}
