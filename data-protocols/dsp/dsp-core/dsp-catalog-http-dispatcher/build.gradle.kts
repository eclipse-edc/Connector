/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *       Cofinity-X - make DSP versions pluggable
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-core:dsp-http-core"))
    api(project(":data-protocols:dsp:dsp-http-spi"))
    api(project(":extensions:common:json-ld"))
    api(project(":spi:control-plane:catalog-spi"))

    testImplementation(testFixtures(project(":data-protocols:dsp:dsp-http-spi")))
}
