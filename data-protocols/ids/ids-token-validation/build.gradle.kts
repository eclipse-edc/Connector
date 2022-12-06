/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":core:common:util"))
    api(project(":data-protocols:ids:ids-spi"))
    api(project(":spi:common:oauth2-spi"))
    api(project(":spi:common:jwt-spi"))

    api(libs.fraunhofer.infomodel)
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
