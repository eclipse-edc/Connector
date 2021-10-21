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
    `maven-publish`
    checkstyle
}

repositories {
    mavenCentral()
}

val jetBrainsAnnotationsVersion: String by project
val jacksonVersion: String by project
val javaVersion: String by project

val securityType by extra { System.getProperty("security.type", "default") }
val iamType by extra { System.getProperty("iam.type", "disabled") }
val configFs by extra { System.getProperty("configuration.fs", "disabled") }
val jupiterVersion: String by project

subprojects {

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
        }
        maven {
            url = uri("https://repository.mulesoft.org/nexus/content/repositories/public/") //used for the multihash lib
        }
    }

    tasks.register<DependencyReportTask>("allDependencies") {}
}

allprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "checkstyle")
    apply(plugin = "java")

    checkstyle {
        toolVersion = "9.0"
        configFile = rootProject.file("resources/edc-checkstyle-config.xml")
        maxErrors = 0 // does not tolerate errors ...
        maxWarnings = 0 // ... or warnings
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    pluginManager.withPlugin("java-library") {
        group = "org.eclipse.dataspaceconnector"
        version = "0.0.1-SNAPSHOT.1"
        dependencies {
            api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
            api("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
            api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

            testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
            testImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
            testImplementation("org.easymock:easymock:4.2")
            testImplementation("org.assertj:assertj-core:3.19.0")

        }

        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/eclipse-datasspaceconnector/dataspaceconnector")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }

    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
    tasks.withType<Test> {
        testLogging {
            events("passed", "skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
    tasks.withType<Checkstyle> {
        reports {
            // lets not generate any reports because that is done from within the Github Actions workflow
            html.required.set(false)
            xml.required.set(false)
        }
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
