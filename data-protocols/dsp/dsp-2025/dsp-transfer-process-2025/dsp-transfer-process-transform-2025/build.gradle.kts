/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Cofinity-X - make DSP versions pluggable
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))
    api(project(":extensions:common:json-ld"))
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    api(project(":data-protocols:dsp:dsp-http-spi"))
    implementation(project(":core:common:lib:jsonld-lib"))
    implementation(project(":data-protocols:dsp:dsp-lib"))

    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":core:common:junit"))
}

