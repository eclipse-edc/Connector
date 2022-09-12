/*
 *  Copyright (c) 2022 Microsoft Corporation
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
import org.hidetake.gradle.swagger.generator.GenerateSwaggerUI


plugins {
    `java-library`
    `maven-publish`
    checkstyle
    jacoco
    signing
    id("com.rameshkp.openapi-merger-gradle-plugin") version "1.0.4"
    id("org.eclipse.dataspaceconnector.module-names")
    id("com.autonomousapps.dependency-analysis") version "1.13.1" apply (false)
    id("org.gradle.crypto.checksum") version "1.4.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.hidetake.swagger.generator") version "2.19.2"
}

repositories {
    mavenCentral()
}

dependencies {
    "swaggerCodegen"("org.openapitools:openapi-generator-cli:6.1.0")
    "swaggerUI"("org.webjars:swagger-ui:4.14.0")
}

val jetBrainsAnnotationsVersion: String by project
val jacksonVersion: String by project
val javaVersion: String by project
val jupiterVersion: String by project
val mockitoVersion: String by project
val assertj: String by project
val rsApi: String by project
val swagger: String by project

val edcDeveloperId: String by project
val edcDeveloperName: String by project
val edcDeveloperEmail: String by project
val edcScmConnection: String by project
val edcWebsiteUrl: String by project
val edcScmUrl: String by project
val groupId: String by project
val defaultVersion: String by project

// required by the nexus publishing plugin
val projectVersion: String = (project.findProperty("edcVersion") ?: defaultVersion) as String

var deployUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

if (projectVersion.contains("SNAPSHOT")) {
    deployUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
}

subprojects {

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
        }
    }
    tasks.register<DependencyReportTask>("allDependencies") {}

    // (re-)create a file that contains all maven publications
    val f = File("${project.rootDir.absolutePath}/docs/developer/modules.md")
    if (f.exists()) {
        f.delete()
    }
    afterEvaluate {
        publishing {
            publications.forEach { i ->
                val mp = (i as MavenPublication)
                mp.pom {
                    name.set(project.name)
                    description.set("edc :: ${project.name}")
                    url.set(edcWebsiteUrl)

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                        developers {
                            developer {
                                id.set(edcDeveloperId)
                                name.set(edcDeveloperName)
                                email.set(edcDeveloperEmail)
                            }
                        }
                        scm {
                            connection.set(edcScmConnection)
                            url.set(edcScmUrl)
                        }
                    }
                }
                f.appendText("\n${mp.groupId}:${mp.artifactId}:${mp.version}")
            }
        }
    }
}

buildscript {
    dependencies {
        classpath("io.swagger.core.v3:swagger-gradle-plugin:2.1.13")
    }
}

allprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "checkstyle")
    apply(plugin = "java")


    apply(plugin = "org.eclipse.dataspaceconnector.test-summary")

    if (System.getenv("JACOCO") == "true") {
        apply(plugin = "jacoco")
    }

    checkstyle {
        toolVersion = "9.0"
        configFile = rootProject.file("resources/edc-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
        maxErrors = 0 // does not tolerate errors
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }

        tasks.withType(JavaCompile::class.java) {
            // making sure the code does not use any APIs from a more recent version.
            // Ref: https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
            options.release.set(javaVersion.toInt())
        }
        withJavadocJar()
        withSourcesJar()
    }

    // EdcRuntimeExtension uses this to determine the runtime classpath of the module to run.
    tasks.register("printClasspath") {
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }

    pluginManager.withPlugin("java-library") {
        group = groupId
        version = projectVersion

        dependencies {
            api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
            api("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
            api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

            testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
            testImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
            testImplementation("org.mockito:mockito-core:${mockitoVersion}")
            testImplementation("org.assertj:assertj-core:${assertj}")
        }

        if (!project.hasProperty("skip.signing")) {

            apply(plugin = "signing")
            publishing {
                repositories {
                    maven {
                        name = "OSSRH"
                        setUrl(deployUrl)
                        credentials {
                            username = System.getenv("OSSRH_USER") ?: return@credentials
                            password = System.getenv("OSSRH_PASSWORD") ?: return@credentials
                        }
                    }
                }

                signing {
                    useGpgCmd()
                    sign(publishing.publications)
                }
            }
        }

    }

    pluginManager.withPlugin("io.swagger.core.v3.swagger-gradle-plugin") {

        dependencies {
            // this is used to scan the classpath and generate an openapi yaml file
            implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:${swagger}")
            implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
        }
// this is used to scan the classpath and generate an openapi yaml file
        tasks.withType<io.swagger.v3.plugins.gradle.tasks.ResolveTask> {
            outputFileName = project.name
            outputFormat = io.swagger.v3.plugins.gradle.tasks.ResolveTask.Format.YAML
            sortOutput = true
            prettyPrint = true
            classpath = java.sourceSets["main"].runtimeClasspath
            buildClasspath = classpath
            resourcePackages = setOf("org.eclipse.dataspaceconnector")
            outputDir = file("${rootProject.projectDir.path}/resources/openapi/yaml")
        }
        configurations {
            all {
                exclude(group = "com.fasterxml.jackson.jaxrs", module = "jackson-jaxrs-json-provider")
            }
        }
    }

    tasks.withType<Test> {
        // Target all type of test e.g. -DrunAllTests="true"
        val runAllTests: String = System.getProperty("runAllTests", "false")
        if (runAllTests == "true") {
            useJUnitPlatform()
        } else {
            // Target specific set of tests by specifying junit tags on command-line e.g. -DincludeTags="tag-name1,tag-name2"
            val includeTagProperty = System.getProperty("includeTags")
            val includeTags: Array<String> = includeTagProperty?.split(",")?.toTypedArray() ?: emptyArray()

            if (includeTags.isNotEmpty()) {
                useJUnitPlatform {
                    includeTags(*includeTags)
                }
            } else {
                useJUnitPlatform {
                    excludeTags("IntegrationTest")
                }
            }
        }

        testLogging {
            if (project.hasProperty("verboseTest")) {
                events("started", "passed", "skipped", "failed", "standard_out", "standard_error")
            } else {
                events("failed")
            }
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    tasks.withType<Checkstyle> {
        reports {
            // lets not generate any reports because that is done from within the Github Actions workflow
            html.required.set(false)
            xml.required.set(true)
        }
    }

    tasks.jar {
        metaInf {
            from("${rootProject.projectDir.path}/LICENSE")
            from("${rootProject.projectDir.path}/NOTICE.md")
        }
    }

    // Generate XML reports for Codecov
    if (System.getenv("JACOCO") == "true") {
        tasks.jacocoTestReport {
            reports {
                xml.required.set(true)
            }
        }
    }
}
openApiMerger {
    val yamlDirectory = file("${rootProject.projectDir.path}/resources/openapi/yaml")

    inputDirectory.set(yamlDirectory)
    output {
        directory.set(file("${rootProject.projectDir.path}/resources/openapi/"))
        fileName.set("openapi")
        fileExtension.set("yaml")
    }
    openApi {
        openApiVersion.set("3.0.1")
        info {
            title.set("EDC REST API")
            description.set("All files merged by open api merger")
            version.set("1.0.0-SNAPSHOT")
            license {
                name.set("Apache License v2.0")
                url.set("http://apache.org/v2")
            }
        }
    }
}

// Dependency analysis active if property "dependency.analysis" is set. Possible values are <'fail'|'warn'|'ignore'>.
if (project.hasProperty("dependency.analysis")) {
    apply(plugin = "org.eclipse.dataspaceconnector.dependency-rules")
    configure<org.eclipse.dataspaceconnector.gradle.DependencyRulesPluginExtension> {
        severity.set(project.property("dependency.analysis").toString())
    }
    apply(plugin = "com.autonomousapps.dependency-analysis")
    configure<com.autonomousapps.DependencyAnalysisExtension> {
        // See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin
        issues {
            all { // all projects
                onAny {
                    severity(project.property("dependency.analysis").toString())
                    exclude(
                        // dependencies declared at the root level for all modules
                        "org.jetbrains:annotations",
                        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310",
                        "com.fasterxml.jackson.core:jackson-core",
                        "com.fasterxml.jackson.core:jackson-annotations",
                        "com.fasterxml.jackson.core:jackson-databind",
                    )
                }
                onUnusedDependencies {
                    exclude(
                        // dependencies declared at the root level for all modules
                        "org.assertj:assertj-core",
                        "org.junit.jupiter:junit-jupiter-api",
                        "org.junit.jupiter:junit-jupiter-params",
                        "org.mockito:mockito-core",
                    )
                }
                onIncorrectConfiguration {
                    exclude(
                        // some common dependencies are intentionally exported by core:base for simplicity
                        "com.squareup.okhttp3:okhttp",
                        "dev.failsafe:failsafe",
                    )
                }
                onUsedTransitiveDependencies {
                    severity("ignore")
                }
            }
        }
        abi {
            exclusions {
                excludeAnnotations(
                    "io\\.opentelemetry\\.extension\\.annotations\\.WithSpan",
                )
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USER") ?: return@sonatype)
            password.set(System.getenv("OSSRH_PASSWORD") ?: return@sonatype)
        }
    }
}

swaggerSources {
    create("edc").apply {
        setInputFile(file("./resources/openapi/openapi.yaml"))
        ui(closureOf<GenerateSwaggerUI> {
            outputDir = file("docs/swaggerui")
            wipeOutputDir = true
        })
    }
}
