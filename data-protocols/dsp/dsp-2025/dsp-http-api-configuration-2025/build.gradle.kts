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
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    api(project(":data-protocols:dsp:dsp-http-spi"))

    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":core:control-plane:control-plane-transform"))

    testImplementation(project(":core:common:junit"))
}
