/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.web.spi;

import jakarta.servlet.Servlet;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Manages the runtime web (HTTP) server.
 */
@ExtensionPoint
public interface WebServer {

    String DEFAULT_CONTEXT_NAME = "default";


    /**
     * Adds a new port mapping and thus a new API context to this web server.
     *
     * @param contextName the name of the API context.
     * @param port        the port of the API context.
     * @param path        the path of the API context.
     */
    void addPortMapping(String contextName, int port, String path);

    /**
     * Adds a new servlet to the specified context name..
     *
     * @param contextName the name of the API context.
     * @param servlet     servlet implementation to add.
     */
    public void registerServlet(String contextName, Servlet servlet);

    /**
     * Returns the default context name
     */
    default String getDefaultContextName() {
        return DEFAULT_CONTEXT_NAME;
    }

}
