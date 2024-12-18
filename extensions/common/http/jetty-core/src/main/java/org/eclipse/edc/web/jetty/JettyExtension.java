/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.web.jetty;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Provides({ WebServer.class, JettyService.class })
public class JettyExtension implements ServiceExtension {


    private static final String DEFAULT_PATH = "/api";
    private static final String DEFAULT_CONTEXT_NAME = "default";
    private static final int DEFAULT_PORT = 8181;
    @Setting
    private static final String KEYSTORE_PATH_SETTING = "edc.web.https.keystore.path";
    @Setting
    private static final String KEYSTORE_TYPE_SETTING = "edc.web.https.keystore.type";

    private JettyService jettyService;
    private final PortMappingRegistryImpl portMappings = new PortMappingRegistryImpl();

    @Configuration
    private JettyConfiguration jettyConfiguration;
    @Configuration
    private DefaultApiConfiguration apiConfiguration;
    @Setting(key = KEYSTORE_PATH_SETTING, description = "Keystore path", required = false)
    private String keystorePath;
    @Setting(key = KEYSTORE_TYPE_SETTING, description = "Keystore type", defaultValue = "PKCS12")
    private String keystoreType;

    @Override
    public String name() {
        return "Jetty Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var defaultPortMapping = new PortMapping(DEFAULT_CONTEXT_NAME, apiConfiguration.port(), apiConfiguration.path());
        portMappings.register(defaultPortMapping);

        var monitor = context.getMonitor();
        KeyStore ks = null;

        if (keystorePath != null) {
            try {
                ks = KeyStore.getInstance(keystoreType);
                try (var stream = new FileInputStream(keystorePath)) {
                    ks.load(stream, jettyConfiguration.keystorePassword().toCharArray());
                }
            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
                throw new EdcException(e);
            }
        }

        jettyService = new JettyService(jettyConfiguration, ks, monitor, portMappings);
        context.registerService(JettyService.class, jettyService);
        context.registerService(WebServer.class, jettyService);
    }

    @Override
    public void start() {
        jettyService.start();
    }

    @Override
    public void shutdown() {
        if (jettyService != null) {
            jettyService.shutdown();
        }
    }

    @Provider
    @Deprecated(since = "0.11.0")
    public WebServiceConfigurer webServiceContextConfigurator(ServiceExtensionContext context) {
        return new WebServiceConfigurerImpl(context.getMonitor(), portMappings);
    }

    @Provider
    public PortMappingRegistry portMappings() {
        return portMappings;
    }

    @Settings
    record DefaultApiConfiguration(
            @Setting(key = "web.http.port", description = "Port for default api context", defaultValue = DEFAULT_PORT + "")
            int port,
            @Setting(key = "web.http.path", description = "Path for default api context", defaultValue = DEFAULT_PATH)
            String path
    ) {

    }

}
