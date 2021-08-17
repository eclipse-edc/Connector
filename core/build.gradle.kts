/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
    api(project(":spi"))
    api(project(":core:bootstrap"))
    api(project(":core:policy:policy-engine"))
    api(project(":core:policy:policy-model"))
    api(project(":core:schema"))
    api(project(":core:protocol:web"))
    api(project(":core:transfer"))
}

publishing {
    publications {
        create<MavenPublication>("core") {
            artifactId = "core"
            from(components["java"])
        }
    }
}
