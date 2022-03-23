package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

open class DataspaceConnectorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.subprojects.forEach { p -> registerTask(p) }
    }

    private fun registerTask(project: Project) {
        project.tasks.register<DependencyRulesTask>("applyDependencyRules") {
            dependencyAnalysis.set("warn") // TODO: initialize from parameter
        }
    }
}