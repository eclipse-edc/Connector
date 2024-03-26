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
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:common:validator-spi"))
    api(project(":spi:common:token-spi"))
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":extensions:common:json-ld"))
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-http-spi"))

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":extensions:common:http:jersey-core"))
}
