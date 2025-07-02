/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.junit.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Spin up a runtime that will be used for a single test
 */
public class RuntimePerMethodExtension extends RuntimeExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    public RuntimePerMethodExtension() {
        this(new EmbeddedRuntime("runtime"));
    }

    public RuntimePerMethodExtension(EmbeddedRuntime runtime) {
        super(runtime);
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        runtime.boot(false);
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) {
        runtime.shutdown();
    }
}
