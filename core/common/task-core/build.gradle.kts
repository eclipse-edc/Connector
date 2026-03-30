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
    api(project(":spi:common:task-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(project(":core:common:lib:store-lib"))
    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(testFixtures(project(":spi:common:task-spi")))
    testImplementation(project(":core:common:junit-base"))

}

