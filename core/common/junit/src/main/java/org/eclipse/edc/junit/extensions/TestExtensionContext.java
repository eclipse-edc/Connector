/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import static org.mockito.Mockito.mock;

/**
 * Extension of the {@link DefaultServiceExtensionContext} that permits to set a configuration, to be used in tests where
 * config settings will be injected in the extension.
 */
public class TestExtensionContext extends DefaultServiceExtensionContext {

    private Config config;

    public TestExtensionContext() {
        super(mock(), ConfigFactory.empty());
        config = ConfigFactory.empty();
    }

    @Override
    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }
}
