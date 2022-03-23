package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.kotlin.dsl.get

private const val TASK_NAME = "applyDependencyRules"

open class EDCGradlePlugin : Plugin<Project> {
    var dependencyAnalysis = "warn"; // TODO: pass as param

    override fun apply(project: Project) {
        project.subprojects.forEach { p -> registerTask(p) }
    }

    private fun registerTask(project: Project) {
        project.tasks.register(TASK_NAME) {
            doLast {
                fun dependencyError(error: String) {
                    val message = "DEPENDENCY RULE VIOLATION: $error"
                    if (dependencyAnalysis == "fail") {
                        throw GradleException(message)
                    } else {
                        println(message)
                    }
                }

                fun dependencyRules(artifact: ResolvedArtifact) {
                    val dependency = artifact.moduleVersion.id
                    if (dependency.group == project.group) {
                        val pathFromRoot = artifact.file.relativeTo(project.rootDir).path
                        val pathFromThisModule = artifact.file.relativeTo(project.projectDir).path

                        if (!dependency.name.endsWith("-spi") // modules may only depend on `-spi` modules (exceptions follow)
                                && dependency.name != "spi" // exception: modules may depend on spi module
                                && !dependency.name.endsWith("-core") // exception: modules may depend on technology libs such as "blob-core"
                                && !pathFromRoot.startsWith("common/") // exception: modules may depend on common module
                                && !pathFromRoot.startsWith("extensions/http/jetty/") // exception: modules might depend on `jetty` (this exception should be removed once there is an SPI for jetty)
                                && !project.path.startsWith(":launchers:") // exception: launchers may depend on other modules
                                && !project.path.startsWith(":samples:") // exception: samples may depend on other modules
                                && !project.path.startsWith(":system-tests:") // exception: system-tests may depend on other modules
                        ) {
                            dependencyError("modules may only depend on '*-spi' modules. Invalid dependency: $dependency")
                        }

                        if (pathFromRoot.startsWith("launchers/") // no module may depend on launchers
                        ) {
                            dependencyError("modules may not depend on launcher modules. Invalid dependency: $dependency")
                        }

                        if (pathFromRoot.startsWith("samples/") // no module may depend on samples (exceptions follow)
                                && !project.path.startsWith(":samples:") // exception: other samples might depend on samples
                        ) {
                            dependencyError("modules may not depend on samples modules. Invalid dependency: $dependency")
                        }

                        if (pathFromRoot.startsWith("system-tests/") // no module may depend on system-tests
                        ) {
                            dependencyError("modules may not depend on system-tests modules. Invalid dependency: $dependency")
                        }

                        if (pathFromThisModule.matches(Regex("\\.\\./[^.].*")) // there should not be "cross-module" dependencies at the same level
                                && !dependency.name.endsWith("-core") // exception: technology libs such as "blob-core"
                        ) {
                            dependencyError("there should not be \"cross-module\" dependencies at the same level. Invalid dependency: $dependency")
                        }

                        if (project.name == "core-spi") { // `core:spi` cannot depend on any other module
                            dependencyError("`core:spi` cannot depend on any other module. Invalid dependency: $dependency")
                        }

                        if (project.name == dependency.name) { // two modules cannot have the same name (TBC)
                            dependencyError("two modules cannot have the same name. Invalid dependency: $dependency")
                        }
                    }
                }

                fun directDependencyRules(dependency: ResolvedDependency) {
                    if (dependency.moduleGroup == project.group) {
                        dependency.moduleArtifacts.forEach { artifact ->
                            val pathFromRoot = artifact.file.relativeTo(project.rootDir).path
                            if (pathFromRoot.startsWith("core/") // no module may depend directly on any core module
                                    && !dependency.name.endsWith("-core") // exception: other core modules
                            ) {
                                dependencyError("modules may not depend directly on core modules. Invalid dependency: ${dependency.name}")
                            }
                        }
                    }
                }

                val compileConfiguration = project.configurations.get("compileClasspath").resolvedConfiguration
                compileConfiguration.resolvedArtifacts.forEach { artifact -> dependencyRules(artifact) }
                compileConfiguration.firstLevelModuleDependencies.forEach { dependency -> directDependencyRules(dependency) }
            }
        }
    }
}