/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:web-spi"))
    api(project(":data-protocols:dsp:dsp-http-spi"))

    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))

    testImplementation(project(":core:common:junit"))
}
