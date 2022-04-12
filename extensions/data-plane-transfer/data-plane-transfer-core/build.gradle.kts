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
}

val nimbusVersion: String by project
val bouncycastleVersion: String by project

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:transfer-spi"))
    api(project(":extensions:data-plane-transfer:data-plane-transfer-spi"))
    api(project(":common:token-generation-lib"))

    api("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    // Note: nimbus requires bouncycastle as mentioned in documentation:
    // https://www.javadoc.io/doc/com.nimbusds/nimbus-jose-jwt/7.2.1/com/nimbusds/jose/jwk/JWK.html#parseFromPEMEncodedObjects-java.lang.String-
    api("org.bouncycastle:bcpkix-jdk15on:${bouncycastleVersion}")
    testImplementation(testFixtures(project(":launchers:junit")))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-transfer-core") {
            artifactId = "data-plane-transfer-core"
            from(components["java"])
        }
    }
}
