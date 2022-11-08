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

package org.eclipse.edc.connector.provision.http;

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.provision.http.webhook.HttpProvisionerWebhookApiController;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.net.MalformedURLException;
import java.net.URL;

@Provides(HttpProvisionerWebhookUrl.class)
public class HttpWebhookExtension implements ServiceExtension {

    public static final String PROVISIONER_WEBHOOK_CONFIG = "web.http.provisioner";
    public static final String PROVISIONER_CONTEXT_ALIAS = "provisioner";

    public static final int DEFUALT_WEBHOOK_PORT = 8383;
    public static final String DEFAULT_PROVISIONER_CONTEXT_PATH = "/api/v1/provisioner";

    public static final String NAME = "Provisioner API";

    @Inject
    private WebServer webServer;

    @Inject
    private WebService webService;

    @Inject
    private AuthenticationService authService;

    @Inject
    private TransferProcessManager transferProcessManager;

    @Inject
    private WebServiceConfigurer configurator;

    @Inject
    private Hostname hostname;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var settings = WebServiceSettings.Builder.newInstance()
                .apiConfigKey(PROVISIONER_WEBHOOK_CONFIG)
                .contextAlias(PROVISIONER_CONTEXT_ALIAS)
                .defaultPath(DEFAULT_PROVISIONER_CONTEXT_PATH)
                .defaultPort(DEFUALT_WEBHOOK_PORT)
                .name(NAME)
                .build();

        var config = configurator.configure(context, webServer, settings);

        registerCallbackUrl(context, config.getPath(), config.getPort());

        webService.registerResource(config.getContextAlias(), new HttpProvisionerWebhookApiController(transferProcessManager));
        webService.registerResource(config.getContextAlias(), new AuthenticationRequestFilter(authService));
    }

    private void registerCallbackUrl(ServiceExtensionContext context, String path, int port) {
        var s = hostname.get();

        if (!s.startsWith("http")) { // a hostname should never have a protocol prefix, but just to be safe
            s = "http://" + s;
        }
        if (!s.matches(".*:([0-9]){1,5}$")) { // a hostname also shouldn't have a port, but again, to be sure
            s += ":" + port;
        }
        s += path + "/callback";
        try {
            var url = new URL(s);
            context.registerService(HttpProvisionerWebhookUrl.class, () -> url);
        } catch (MalformedURLException e) {
            context.getMonitor().severe("Error creating callback endpoint", e);
            throw new EdcException(e);
        }
    }

}
