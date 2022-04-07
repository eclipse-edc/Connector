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

import org.gradle.api.provider.Property;

/**
 * Configuration for {@link DependencyRulesPlugin}.
 */
public abstract class DependencyRulesPluginExtension {

    /**
     * Severity for rule violations.
     *
     * Use "fail" to fail the build whenever a rule is violated. Any other value will generate warnings.
     *
     * @return Severity
     */
    public abstract Property<String> getSeverity();

    public boolean isFailSeverity() {
        return "fail".equals(getSeverity().get());
    }
}
