/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("com.fasterxml.jackson.datatype:jackson-datatype-jakarta-jsonp:2.13.4")
    api("com.apicatalog:titanium-json-ld:1.3.1")
    api("org.glassfish:jakarta.json:2.0.0")

    api(project(":spi:common:core-spi"))
    api(project(":spi:common:catalog-spi"))
    api(project(":spi:common:transform-spi"))
    api(project(":spi:control-plane:contract-spi"))

    implementation("org.jetbrains:annotations:15.0")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.8.1")
        }
    }
}