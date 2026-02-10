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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:participant-context-single-spi"))
    api(project(":spi:common:participant-context-config-spi"))

    testImplementation(project(":core:common:junit-base"))
    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:common:participant-context-config-spi")))

}
