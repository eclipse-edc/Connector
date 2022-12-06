/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:common:web-spi"))

    api(project(":spi:control-plane:transfer-data-plane-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":extensions:data-plane:data-plane-client"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))

    implementation(project(":extensions:common:api:control-api-configuration"))
    implementation(project(":core:common:jwt-core"))

    api(libs.jakarta.rsApi)
    api(libs.nimbus.jwt)
    // Note: nimbus requires bouncycastle as mentioned in documentation:
    // https://www.javadoc.io/doc/com.nimbusds/nimbus-jose-jwt/7.2.1/com/nimbusds/jose/jwk/JWK.html#parseFromPEMEncodedObjects-java.lang.String-
    api(libs.bouncyCastle.bcpkix)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.jersey.multipart)
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
