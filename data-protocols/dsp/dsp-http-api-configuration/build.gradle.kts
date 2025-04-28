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
    api(project(":spi:control-plane:catalog-spi"))
    api(project(":spi:common:core-spi"))
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-http-spi"))

    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":core:control-plane:control-plane-transform"))

    testImplementation(project(":core:common:junit"))
}
