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
    `maven-publish`
}

dependencies {
    api(project(":spi:decentralized-claims-spi"))
    api(project(":spi:core-spi"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:core-spi")))
}
