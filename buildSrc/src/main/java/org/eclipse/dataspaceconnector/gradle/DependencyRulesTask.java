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

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Custom gradle task to run dependency rules.
 */
public abstract class DependencyRulesTask extends DefaultTask {

    private static final Pattern CROSS_MODULE_DEPENDENCY_PATTERN = Pattern.compile("\\.\\./[^.].*");

    /**
     * Flag to specify if build should fail on dependency rule violation
     *
     * @return True if fail on dependency rule violation
     */
    @Input
    public abstract Property<Boolean> getFailOnError();

    @TaskAction
    public void check() {
        var compileConfiguration = this.getProject().getConfigurations().getByName("compileClasspath").getResolvedConfiguration();
        compileConfiguration.getResolvedArtifacts().forEach(this::dependencyRules);
        compileConfiguration.getFirstLevelModuleDependencies().forEach(this::directDependencyRules);
    }

    private void dependencyRules(ResolvedArtifact artifact) {
        var project = this.getProject();
        var dependency = artifact.getModuleVersion().getId();

        if (dependency.getGroup().equals(this.getProject().getGroup())) {
            var pathFromRoot = project.getRootDir().toPath().relativize(artifact.getFile().toPath());
            var pathFromThisModule = project.getProjectDir().toPath().relativize(artifact.getFile().toPath());

            if (!dependency.getName().endsWith("-spi") && // modules may only depend on `-spi` modules (exceptions follow)
                    !dependency.getName().equals("spi") && // exception: modules may depend on spi module
                    !dependency.getName().endsWith("-core") && // exception: modules may depend on technology libs such as "blob-core"
                    !pathFromRoot.startsWith("common/") && // exception: modules may depend on common module
                    !pathFromRoot.startsWith("extensions/http/jetty/") && // exception: modules might depend on `jetty` (this exception should be removed once there is an SPI for jetty)
                    !project.getPath().startsWith(":launchers:") && // exception: launchers may depend on other modules
                    !project.getPath().startsWith(":samples:") && // exception: samples may depend on other modules
                    !project.getPath().startsWith(":system-tests:") // exception: system-tests may depend on other modules
            ) {
                dependencyError(format("modules may only depend on '*-spi' modules. Invalid dependency: %s", dependency));
            }

            if (pathFromRoot.startsWith("launchers/") // no module may depend on launchers
            ) {
                dependencyError(format("modules may not depend on launcher modules. Invalid dependency: %s", dependency));
            }

            if (pathFromRoot.startsWith("samples/") && // no module may depend on samples (exceptions follow)
                    !project.getPath().startsWith(":samples:") // exception: other samples might depend on samples
            ) {
                dependencyError(format("modules may not depend on samples modules. Invalid dependency: %s", dependency));
            }

            if (pathFromRoot.startsWith("system-tests/") // no module may depend on system-tests
            ) {
                dependencyError(format("modules may not depend on system-tests modules. Invalid dependency: %s", dependency));
            }

            if (CROSS_MODULE_DEPENDENCY_PATTERN.matcher(pathFromThisModule.toString()).matches() && // there should not be "cross-module" dependencies at the same level
                    !dependency.getName().endsWith("-core") && // exception: technology libs such as "blob-core"
                    !pathFromRoot.startsWith("samples/") // exception: samples may refer to own modules
            ) {
                dependencyError(format("there should not be \"cross-module\" dependencies at the same level. Invalid dependency: %s", dependency));
            }

            if (project.getName().equals("core-spi")) { // `core:spi` cannot depend on any other module
                dependencyError(format("core:spi` cannot depend on any other module. Invalid dependency: %s", dependency));
            }

            if (project.getName().equals(dependency.getName())) { // two modules cannot have the same name (TBC)
                dependencyError(format("two modules cannot have the same name. Invalid dependency: %s", dependency));
            }
        }
    }

    private void directDependencyRules(ResolvedDependency dependency) {
        var project = this.getProject();

        if (dependency.getModuleGroup().equals(project.getGroup())) {
            dependency.getModuleArtifacts().forEach(artifact -> {
                var pathFromRoot = project.getRootDir().toPath().relativize(artifact.getFile().toPath());
                if (pathFromRoot.startsWith("core/") && // no module may depend directly on any core module
                        !dependency.getName().endsWith("-core") // exception: other core modules
                ) {
                    dependencyError(format("modules may not depend directly on core modules. Invalid dependency: %s", dependency.getName()));
                }
            });
        }
    }

    private void dependencyError(String error) {
        var message = format("DEPENDENCY RULE VIOLATION: %s", error);
        if (getFailOnError().get()) {
            throw new GradleException(message);
        } else {
            getLogger().warn(message);
        }
    }
}
