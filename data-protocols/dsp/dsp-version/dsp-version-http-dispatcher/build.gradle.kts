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
 *       Cofinity-X - make DSP versions pluggable
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-core:dsp-http-core"))
    api(project(":data-protocols:dsp:dsp-http-spi"))
    api(project(":spi:control-plane:protocol-version-spi"))

    testImplementation(testFixtures(project(":data-protocols:dsp:dsp-http-spi")))
}
