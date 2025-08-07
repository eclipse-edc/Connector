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
    api(project(":spi:common:core-spi"))
    api(project(":data-protocols:dsp:dsp-08:dsp-spi-08"))

    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-transform-lib"))
}
