/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Catena-X Consortium - initial API and implementation
 *      Fraunhofer Institute for Software and Systems Engineering - added dependency
 *
 */

plugins {
    `java-library`
}


dependencies {
    implementation(root.jetty.websocket)

    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))

    testImplementation(root.bundles.jersey.core)
    testImplementation(root.restAssured)
    testImplementation(project(":core:common:junit"))
}


