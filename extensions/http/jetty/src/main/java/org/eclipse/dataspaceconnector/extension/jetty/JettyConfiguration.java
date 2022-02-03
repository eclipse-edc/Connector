/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Config;

import java.util.HashMap;
import java.util.Map;

public class JettyConfiguration {

    public static final String WEB_HTTP_PREFIX = "web.http";
    @EdcSetting
    private static final String HTTP_PORT = "web.http.port";
    private final String keystorePassword;
    private final String keymanagerPassword;
    private final Map<String, PortMapping> portMappings;

    public JettyConfiguration(String keystorePassword, String keymanagerPassword) {
        this.keystorePassword = keystorePassword;
        this.keymanagerPassword = keymanagerPassword;
        portMappings = new HashMap<>();
    }

    public static JettyConfiguration createFromConfig(String keystorePassword, String keymanagerPassword, Config config) {
        var jettyConfig = new JettyConfiguration(keystorePassword, keymanagerPassword);

        var defaultPort = config.getInteger(HTTP_PORT, null);

        // the default setting was used, no context specific port mappings
        if (defaultPort != null) {
            jettyConfig.portMapping("default", defaultPort, "/api");
        }

        // we have potentially multiple jetty port configs
        var subConfig = config.getConfig(WEB_HTTP_PREFIX);

        subConfig.getEntries().keySet().forEach(key -> {
            if (HTTP_PORT.equals(key)) { // file the web.http.port under web.http.default.port
                var defaultPath = config.getString("web.http.path", "/api");
                var defaultName = "default";
                jettyConfig.portMapping(defaultName, config.getInteger(key), defaultPath);
            } else if (key.endsWith(".port")) {
                var name = key.replace(WEB_HTTP_PREFIX + ".", ""); //chop off leading web.http.
                name = name.substring(0, name.lastIndexOf(".")); // for example web.http.something.port --> something
                if (jettyConfig.portMappings.containsKey(name)) {
                    throw new IllegalArgumentException("A default configuration was already specified. Please use either 'web.http.port' or 'web.http.default.port', not both!");
                }
                var path = config.getString(WEB_HTTP_PREFIX + "." + name + ".path", "/api");
                jettyConfig.portMapping(name, config.getInteger(key), path);
            }
        });

        if (jettyConfig.getPortMappings().isEmpty()) {
            jettyConfig.portMapping(new PortMapping());
        }
        return jettyConfig;
    }

    public Map<String, PortMapping> getPortMappings() {
        return portMappings;
    }

    public JettyConfiguration portMapping(String name, int port, String path) {
        portMappings.put(name, new PortMapping(name, port, path));
        return this;
    }

    public JettyConfiguration portMapping(PortMapping mapping) {
        portMappings.put(mapping.getName(), mapping);
        return this;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeymanagerPassword() {
        return keymanagerPassword;
    }

    public static class PortMapping {
        private final String name;
        private final int port;
        private final String path;

        public PortMapping() {
            this("default", 8181, "/api");
        }

        public PortMapping(String name, int port, String path) {
            this.name = name;
            this.port = port;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public int getPort() {
            return port;
        }

        public String getPath() {
            return path;
        }
    }
}
