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
    api(project(":spi:common:cel-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:verifiable-credentials-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:policy-monitor:policy-monitor-spi"))

    implementation(project(":core:common:lib:store-lib"))
    implementation(libs.cel)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:common:cel-spi")))
    testImplementation(project(":core:common:lib:query-lib"))

}

