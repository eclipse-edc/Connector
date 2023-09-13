/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 * Utility class that permits to run multiple EDC runtimes statically
 */
public class EdcClassRuntimesExtension implements BeforeAllCallback, AfterAllCallback {

    public final EdcRuntimeExtension[] extensions;

    public EdcClassRuntimesExtension(EdcRuntimeExtension... extensions) {
        this.extensions = extensions;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        for (var extension : this.extensions) {
            try {
                extension.beforeTestExecution(extensionContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        for (var extension : this.extensions) {
            extension.afterTestExecution(extensionContext);
        }
    }
}
