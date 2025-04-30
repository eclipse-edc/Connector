/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Cofinity-X - refactor DSP module structure to make versions pluggable
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-core:dsp-http-core"))
    api(project(":data-protocols:dsp:dsp-http-spi"))
    api(project(":extensions:common:json-ld"))
    api(project(":spi:control-plane:contract-spi"))

    testImplementation(testFixtures(project(":data-protocols:dsp:dsp-http-spi")))
}
