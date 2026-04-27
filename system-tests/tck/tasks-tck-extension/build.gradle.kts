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
    java
}

dependencies {
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:common:connector-participant-context-spi"))
    api(project(":spi:common:participant-context-config-spi"))
    api(project(":spi:common:task-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:common:transaction-spi"))
    implementation(libs.nimbus.jwt)
}

