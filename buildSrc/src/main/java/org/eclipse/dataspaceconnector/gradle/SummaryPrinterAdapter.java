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

import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

/**
 * Adapter for the {@link TestListener}, that allows overriding just one method.
 * Currently, only the {@link TestListener#afterSuite(TestDescriptor, TestResult)} is used.
 */
public abstract class SummaryPrinterAdapter implements TestListener {
    @Override
    public void beforeSuite(TestDescriptor suite) {

    }

    @Override
    public void afterSuite(TestDescriptor suite, TestResult result) {

    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {

    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {

    }
}
