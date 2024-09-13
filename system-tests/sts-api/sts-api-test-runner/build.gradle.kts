/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    java
}

dependencies {
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:common:identity-trust-sts-spi"))
    testImplementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:lib:identity-trust-sts-remote-lib"))
    testImplementation(project(":extensions:common:iam:oauth2:oauth2-client"))

    testImplementation(libs.restAssured)
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.junit.jupiter.api)

    testCompileOnly(project(":system-tests:sts-api:sts-api-test-runtime"))
    testImplementation(testFixtures(project(":spi:common:identity-trust-sts-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))
    testImplementation(project(":extensions:common:transaction:transaction-local"))
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
}

edcBuild {
    publish.set(false)
}
