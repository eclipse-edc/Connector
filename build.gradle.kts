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


plugins {
    `java-library`
    // todo: remove once https://github.com/eclipse-edc/Connector/issues/2191 is complete
    id("org.hidetake.swagger.generator") version "2.19.2"
}

val javaVersion: String by project
val edcScmConnection: String by project
val edcWebsiteUrl: String by project
val edcScmUrl: String by project
val groupId: String by project
val defaultVersion: String by project
val annotationProcessorVersion: String by project
val metaModelVersion: String by project

var actualVersion: String = (project.findProperty("version") ?: defaultVersion) as String
if (actualVersion == "unspecified") {
    actualVersion = defaultVersion
}

buildscript {
    dependencies {
        val edcGradlePluginsVersion: String by project
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:${edcGradlePluginsVersion}")
    }
}

dependencies {
    "swaggerUI"("org.webjars:swagger-ui:4.15.5")
}

allprojects {
    apply(plugin = "${groupId}.edc-build")

    // configure which version of the annotation processor to use. defaults to the same version as the plugin
    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(annotationProcessorVersion)
        outputDirectory.set(project.buildDir)
    }

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        versions {
            // override default dependency versions here
            projectVersion.set(actualVersion)
            metaModel.set(metaModelVersion)

        }
        pom {
            projectName.set(project.name)
            description.set("edc :: ${project.name}")
            projectUrl.set(edcWebsiteUrl)
            scmConnection.set(edcScmConnection)
            scmUrl.set(edcScmUrl)
        }
        swagger {
            title.set("EDC REST API")
            description = "EDC REST APIs - merged by OpenApiMerger"
            outputFilename.set(project.name)
            outputDirectory.set(file("${rootProject.projectDir.path}/resources/openapi/yaml"))
        }
        javaLanguageVersion.set(JavaLanguageVersion.of(javaVersion))
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/edc-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }


    // EdcRuntimeExtension uses this to determine the runtime classpath of the module to run.
    tasks.register("printClasspath") {
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }

}
repositories {
    mavenCentral()
}

// Dependency analysis active if property "dependency.analysis" is set. Possible values are <'fail'|'warn'|'ignore'>.
if (project.hasProperty("dependency.analysis")) {
    apply(plugin = "org.eclipse.edc.dependency-rules")
    configure<org.eclipse.edc.gradle.DependencyRulesPluginExtension> {
        severity.set(project.property("dependency.analysis").toString())
    }
}

// todo: remove once https://github.com/eclipse-edc/Connector/issues/2191 is complete
swaggerSources {
    create("edc").apply {
        setInputFile(file("./resources/openapi/openapi.yaml"))
        ui(closureOf<org.hidetake.gradle.swagger.generator.GenerateSwaggerUI> {
            outputDir = file("docs/swaggerui")
            wipeOutputDir = true
        })
    }
}
