/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

val jetBrainsAnnotationsVersion: String by project
val jacksonVersion: String by project

val securityType by extra { System.getProperty("security.type", "default") }
val iamType by extra { System.getProperty("iam.type", "disabled") }
val configFs by extra { System.getProperty("configuration.fs", "disabled") }

subprojects {

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
        }
    }

}

allprojects {
    apply(plugin = "maven-publish")
    pluginManager.withPlugin("java-library") {
        group = "com.microsoft"
        version = "0.0.1-SNAPSHOT.1"
        dependencies {
            api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
            api("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
            api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

            testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
            testImplementation("org.easymock:easymock:4.2")
            testImplementation("org.assertj:assertj-core:3.19.0")

        }

        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/microsoft/data-appliance-gx")
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
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}