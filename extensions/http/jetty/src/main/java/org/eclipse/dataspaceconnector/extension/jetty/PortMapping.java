/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.extension.jetty;

/**
 * POJO that contains portmappings for Jetty, consisting of a context alias, a port and a path.
 *
 * @see JettyConfiguration
 * @see JettyService
 */
public class PortMapping {
    private final String alias;
    private final int port;
    private final String path;

    public PortMapping() {
        this(JettyConfiguration.DEFAULT_CONTEXT_NAME, JettyConfiguration.DEFAULT_PORT, JettyConfiguration.DEFAULT_PATH);
    }

    public PortMapping(String name, int port, String path) {
        alias = name;
        this.port = port;
        this.path = path;
    }

    public String getName() {
        return alias;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "{" +
                "alias='" + alias + '\'' +
                ", port=" + port +
                ", path='" + path + '\'' +
                '}';
    }
}
