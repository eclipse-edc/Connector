/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
    api(project(":extensions:api:api-core"))
    api(project(":extensions:api:data-management:contractdefinition"))
    api(project(":extensions:api:data-management:transferprocess"))
    api(project(":extensions:api:data-management:contractnegotiation"))
    api(project(":extensions:api:data-management:policydefinition"))
    api(project(":extensions:api:data-management:transferprocess"))
    api(project(":extensions:api:data-management:asset"))
    api(project(":extensions:api:data-management:api-configuration"))
}

publishing {
    publications {
        create<MavenPublication>("data-management-api") {
            artifactId = "data-management-api"
            from(components["java"])
        }
    }
}
