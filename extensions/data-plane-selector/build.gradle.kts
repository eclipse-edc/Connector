/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":extensions:data-plane-selector:selector-api"))
    api(project(":extensions:data-plane-selector:selector-spi"))
    api(project(":extensions:data-plane-selector:selector-core"))
    api(project(":extensions:data-plane-selector:selector-store"))
    api(project(":extensions:data-plane-selector:selector-client"))
}