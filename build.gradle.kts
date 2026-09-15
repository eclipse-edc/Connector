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

// From the tag to the Maven coordinate, ONLY the last field is dropped -- the
// `a.b.c` maturity counter, which does not belong to the base coordinate -- and
// the leading `v`, because Maven versions do not carry it.
//
//   v 1.0.0-RC1 - 1 - 0.0.1   ->   Maven  1.0.0-RC1-1
//     └────┬───┘  │   └─┬─┘
//          │      │     └── maturity counter; does NOT travel to the coordinate
//          │      └──────── freeze iteration over THAT upstream tag (starts at 1)
//          └─────────────── the upstream tag, VERBATIM
//
// Dropping the last field and keeping the other two is the point: a launcher
// can iterate its maturity -- add an extension of its own -- without the base
// being republished under a new coordinate, and the name still says which
// upstream release it came from.
//
// `.+` is deliberate, not lax: the upstream tag is kept verbatim, dashes and
// capitals included (`1.0.0-RC1`), so the only thing that can be anchored is
// the tail. That is why the last two fields are explicit.
//
// TWIN IN BASH: the launchers rebuild this same cut in
// `scripts/verificar-congelacion.sh` (the grammar guard and the `${tag%-*}`
// cut). Change one, change the other.
//
//   v1.0.0-RC1-1-0.0.1  -> 1.0.0-RC1-1     v1.0.0-RC1-2-0.0.3 -> 1.0.0-RC1-2
//   v1.0.0-1-0.0.1      -> 1.0.0-1         v1.1.0-1-0.1.0     -> 1.1.0-1
//   v1.0.0-RC1          -> NO VERSION      (upstream tag, not ours)
//   v0.18.0             -> NO VERSION      (idem)
val maturity = """0\.0\.[1-9]\d*|0\.[1-9]\d*\.0|[1-9]\d*\.0\.0"""
val grammar = Regex("""^v(.+-\d+)-(?:$maturity)$""")

// An upstream tag does not match, and that is the mechanism that prevents
// publishing from the mirror believing it is one of ours.
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
