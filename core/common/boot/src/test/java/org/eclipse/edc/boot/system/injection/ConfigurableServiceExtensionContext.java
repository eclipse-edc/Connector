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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import static org.mockito.Mockito.mock;

/**
 * {@link DefaultServiceExtensionContext} which configuration can be changed, to be used in tests where config settings
 * will be injected.
 */
class ConfigurableServiceExtensionContext extends DefaultServiceExtensionContext {

    private Config config = ConfigFactory.empty();

    ConfigurableServiceExtensionContext() {
        super(mock(), ConfigFactory.empty());
    }

    @Override
    public Config getConfig() {
        return config;
    }

    void setConfig(Config config) {
        this.config = config;
    }
}
