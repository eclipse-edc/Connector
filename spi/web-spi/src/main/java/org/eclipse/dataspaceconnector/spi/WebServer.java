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

package org.eclipse.dataspaceconnector.spi;

import org.eclipse.dataspaceconnector.spi.system.Feature;

/**
 * Manages the runtime web (HTTP) server.
 */
@Feature("edc:core:base:webserver")
public interface WebServer {
    
    /**
     * Adds a new port mapping and thus a new API context to this web server.
     *
     * @param contextName the name of the API context.
     * @param port the port of the API context.
     * @param path the path of the API context.
     */
    void addPortMapping(String contextName, int port, String path);
    
}
