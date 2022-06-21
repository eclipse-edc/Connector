/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

import static java.lang.String.format;

/**
 * Plugin that adds a {@link TestListener} to each "test" task in the project, that prints a little summary.
 */
public class TestSummaryPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        var testTasks = target.getTasks().getByName("test");
        if (testTasks instanceof Test) {
            ((Test) testTasks).addTestListener(new AfterSuitePrinter(target.getLogger()));
        }
    }

    private class AfterSuitePrinter extends SummaryPrinterAdapter {
        private final Logger logger;

        AfterSuitePrinter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void afterSuite(TestDescriptor suite, TestResult result) {
            if (suite.getParent() == null) { // will match the outermost suite
                var output = format("Results: %s (%d tests, %d passed, %d failed, %d skipped)", result.getResultType().toString(),
                        result.getTestCount(), result.getSuccessfulTestCount(), result.getFailedTestCount(), result.getSkippedTestCount());
                var startItem = "|  ";
                var endItem = "  |";
                var repeatLength = startItem.length() + output.length() + endItem.length();
                logger.lifecycle(format("\n%s\n%s%s%s\n%s%n", "-".repeat(repeatLength), startItem, output, endItem, "-".repeat(repeatLength)));
            }
        }
    }
}
