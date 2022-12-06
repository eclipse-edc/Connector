/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.web.spi.configuration;


import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;

/**
 * Configure an API extension
 */
@ExtensionPoint
public interface WebServiceConfigurer {

    /**
     * Build the configuration for an API
     *
     * @param context The context
     * @param webServer The WebServer
     * @param settings WebService settings
     * @return The final webservice configuration
     */
    WebServiceConfiguration configure(ServiceExtensionContext context, WebServer webServer, WebServiceSettings settings);
}
