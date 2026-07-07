/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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
    api(project(":spi:core-spi"))

    implementation(project(":core:control-plane:lib:control-plane-lib"))

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(testFixtures(project(":spi:core-spi")))
}
