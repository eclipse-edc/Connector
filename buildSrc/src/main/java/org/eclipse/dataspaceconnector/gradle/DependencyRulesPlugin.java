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

package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Custom grade plugin to run EDC dependency rules.
 * Use the "applyDependencyRules" task registered by this plugin to run the dependency rules.
 */
public class DependencyRulesPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions()
                .create("dependencyrulespluginextension", DependencyRulesPluginExtension.class);
        project.getSubprojects().forEach(p -> registerTask(p, extension));
    }

    private void registerTask(Project project, DependencyRulesPluginExtension extension) {
        project.getTasks()
                .register("applyDependencyRules", DependencyRulesTask.class)
                .configure(task -> task.getFailOnError().set(extension.isFailSeverity()));
    }
}