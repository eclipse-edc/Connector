/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Custom grade plugin to avoid module name duplications.
 * Checks between modules with a gradle build file that their names are unique in the whole project.
 * `samples` and `system-tests` modules are excluded.
 *
 * Ref: https://github.com/gradle/gradle/issues/847
 */
public class ModuleNamesPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.afterEvaluate(new ModuleNamesAction());
    }

    private class ModuleNamesAction implements Action<Project> {

        private final Predicate<String> isSampleModule = displayName -> displayName.startsWith("project ':samples");
        private final Predicate<String> isSystemTestModule = displayName -> displayName.startsWith("project ':system-tests");
        private final Predicate<String> excludeSamplesAndSystemTests = isSampleModule.or(isSystemTestModule).negate();

        @Override
        public void execute(Project project) {
            var subprojects = project.getSubprojects().stream()
                    .filter(it -> it.getBuildFile().exists())
                    .map(Project::getDisplayName)
                    .filter(excludeSamplesAndSystemTests)
                    .collect(groupingBy(projectName));

            var duplicatedSubprojects = subprojects.entrySet().stream()
                    .filter(it -> it.getValue().size() > 1)
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!duplicatedSubprojects.isEmpty()) {
                var message = duplicatedSubprojects.entrySet().stream()
                        .map(it -> it.getKey() + ":\n" + it.getValue().stream().collect(joining("\n\t", "\t", "")))
                        .collect(joining("\n"));

                throw new GradleException("Duplicated module names found: \n" + message);
            }
        }

        private final Function<String, String> projectName = it -> {
            var split = it.replace("project '", "").replace("'", "").split(":");
            return split[split.length - 1];
        };
    }
}
