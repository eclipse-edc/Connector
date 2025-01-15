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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Spin up a static runtime to be used for multiple tests
 */
public class RuntimePerClassExtension extends RuntimeExtension implements BeforeAllCallback, AfterAllCallback {

    public RuntimePerClassExtension() {
        this(new EmbeddedRuntime("runtime"));
    }

    public RuntimePerClassExtension(EmbeddedRuntime runtime) {
        super(runtime);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        runtime.boot(false);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        runtime.shutdown();
    }
}
