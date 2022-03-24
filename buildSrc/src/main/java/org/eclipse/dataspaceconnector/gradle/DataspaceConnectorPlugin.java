package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DataspaceConnectorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create(
            "dataspaceconnectorplugin", DataspaceConnectorPluginExtension.class
        );
        project.getSubprojects().forEach(p -> registerTask(p, extension));
    }

    private void registerTask(Project project, DataspaceConnectorPluginExtension extension) {
        project.getTasks()
            .register("applyDependencyRules", DependencyRulesTask.class)
            .configure(task -> task.getFailOnError().set(extension.isFailSeverity()));
    }
}