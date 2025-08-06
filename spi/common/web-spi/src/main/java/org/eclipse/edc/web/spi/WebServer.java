/*
 *  Copyright (c) 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
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

    /**
     * Adds a new servlet to the specified context name..
     *
     * @param contextName the name of the API context.
     * @param servlet     servlet implementation to add.
     */
    void registerServlet(String contextName, Servlet servlet);

}
