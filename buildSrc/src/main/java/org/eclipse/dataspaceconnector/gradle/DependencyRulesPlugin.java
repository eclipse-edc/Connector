package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DependencyRulesPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create(
                "dataspaceconnectorplugin", DependencyRulesPluginExtension.class
        );
        project.getSubprojects().forEach(p -> registerTask(p, extension));
    }

    private void registerTask(Project project, DependencyRulesPluginExtension extension) {
        project.getTasks()
                .register("applyDependencyRules", DependencyRulesTask.class)
                .configure(task -> task.getFailOnError().set(extension.isFailSeverity()));
    }
}