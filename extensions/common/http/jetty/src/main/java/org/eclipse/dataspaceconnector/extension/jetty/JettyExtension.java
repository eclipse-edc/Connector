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

package org.eclipse.dataspaceconnector.extension.jetty;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebServer;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Provides({ WebServer.class, JettyService.class })
public class JettyExtension implements ServiceExtension {


    @EdcSetting
    private static final String KEYSTORE_PASSWORD = "edc.web.https.keystore.password";
    @EdcSetting
    private static final String KEYMANAGER_PASSWORD = "edc.web.https.keymanager.password";
    @EdcSetting
    private static final String KEYSTORE_PATH_SETTING = "edc.web.https.keystore.path";
    @EdcSetting
    private static final String KEYSTORE_TYPE_SETTING = "edc.web.https.keystore.type";

    private JettyService jettyService;

    @Override
    public String name() {
        return "Jetty Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        KeyStore ks = null;
        var keystorePath = context.getConfig().getString(KEYSTORE_PATH_SETTING, null);
        var configuration = JettyConfiguration.createFromConfig(context.getSetting(KEYSTORE_PASSWORD, "password"), context.getSetting(KEYMANAGER_PASSWORD, "password"), context.getConfig());

        if (keystorePath != null) {
            try {
                ks = KeyStore.getInstance(context.getSetting(KEYSTORE_TYPE_SETTING, "PKCS12"));
                try (var stream = new FileInputStream(keystorePath)) {
                    ks.load(stream, configuration.getKeystorePassword().toCharArray());
                }
            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
                throw new EdcException(e);
            }
        }


        jettyService = new JettyService(configuration, ks, monitor);
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

}
