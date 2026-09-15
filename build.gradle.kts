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

// MODIFIED from upstream eclipse-edc (Apache-2.0) in this fork: the version is
// derived from the git tag and the autodoc/signing setup is adapted for a frozen
// copy. See the commit on branch freeze/v1.0.0-RC1-1. Upstream LICENSE and
// NOTICE.md apply unchanged.


plugins {
    `java-library`
    alias(libs.plugins.edc.build)
}

val edcScmUrl: String by project
val edcScmConnection: String by project

buildscript {
    dependencies {
        val autodocVersion: String by project
        classpath("org.eclipse.edc.autodoc:org.eclipse.edc.autodoc.gradle.plugin:$autodocVersion")
    }
}

// The version comes from the git tag, never from a file. See gradle.properties.
val gitTag: String = providers.exec {
    // --match "v*" on purpose: only RELEASE tags yield a version. Without the
    // filter, a descriptive tag such as `upstream/2026-01-01` would become the
    // artifact version.
    commandLine("git", "describe", "--tags", "--exact-match", "--match", "v*")
    isIgnoreExitValue = true
}.standardOutput.asText.get().trim()

// From the tag to the Maven coordinate only the leading `v` is dropped: Maven
// versions do not carry it. The tag itself is `v<upstream>-<n>`, the same
// convention Debian and RPM use for downstream builds of an upstream release:
//
//   v 1.0.0-RC1 - 1        ->   Maven  1.0.0-RC1-1
//     └────┬───┘  │
//          │      └──────── revision of THIS fork over that upstream tag (starts at 1);
//          │                it moves only when the fork's delta changes
//          └─────────────── the upstream tag, VERBATIM, dashes and capitals included
//
// `.+` is deliberate, not lax: the upstream tag may contain dashes, so the only
// thing that can be anchored is the tail `-<n>`. An upstream tag (`v1.0.0-RC1`,
// `v0.18.0`) does not match -- its last field is not an integer after a dash --
// and that is the mechanism that prevents publishing from the mirror believing
// it is one of ours.
//
//   v1.0.0-RC1-1   -> 1.0.0-RC1-1      v1.0.0-2  -> 1.0.0-2
//   v1.0.0-RC1     -> NO VERSION       v0.18.0   -> NO VERSION
val grammar = Regex("""^v(.+-\d+)$""")
val releaseVersion: String = grammar.find(gitTag)?.groupValues?.get(1) ?: ""

allprojects { version = releaseVersion.ifEmpty { "0.0.0-UNTAGGED" } }

val autodocVersion: String by project

val edcBuildId = libs.plugins.edc.build.get().pluginId

allprojects {
    apply(plugin = edcBuildId)
    apply(plugin = "org.eclipse.edc.autodoc")

    // The autodoc plugin defaults its annotation processor to the project
    // version. With a frozen version of our own that artifact does not exist
    // upstream and the build dies on the first compileJava.
    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(autodocVersion)
    }

    // edc-build applies the vanniktech publisher, which calls
    // signAllPublications() regardless of -Pskip.signing. This is a frozen copy
    // that never goes to Maven Central, so there is no GPG key: signing is
    // disabled so the tasks are skipped instead of breaking the build.
    tasks.withType<org.gradle.plugins.signing.Sign>().configureEach {
        enabled = false
    }

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom {
            scmUrl.set(edcScmUrl)
            scmConnection.set(edcScmConnection)
        }
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/edc-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }

}
