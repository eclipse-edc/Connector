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
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.provision.http.webhook.HttpProvisionerWebhookApiController;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;

@Provides(HttpProvisionerWebhookUrl.class)
@Extension(HttpWebhookExtension.NAME)
public class HttpWebhookExtension implements ServiceExtension {
    public static final String NAME = "HTTP Webhook";
    private static final String DEPRECATED_PROVISIONER_WEBHOOK_CONFIG = "web.http.provisioner";

    /**
     * This deprecation is used to permit a softer transition from the deprecated `web.http.provisioner` config group to
     * the current `web.http.management`
     *
     * @deprecated "web.http.management" config should be used instead of "web.http.provisioner"
     */
    @Deprecated(since = "milestone8")
    public static final WebServiceSettings DEPRECATED_SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(DEPRECATED_PROVISIONER_WEBHOOK_CONFIG)
            .contextAlias("provisioner")
            .defaultPath("/api/v1/provisioner")
            .defaultPort(8383)
            .name("Provisioner API")
            .build();

    @Inject
    private WebServer webServer;

    @Inject
    private WebService webService;

    @Inject
    private AuthenticationService authService;

    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private WebServiceConfigurer configurator;

    @Inject
    private Hostname hostname;

    @Inject
    private ManagementApiConfiguration managementApiConfiguration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        WebServiceConfiguration webServiceConfiguration;
        if (context.getConfig().hasPath(DEPRECATED_PROVISIONER_WEBHOOK_CONFIG)) {
            webServiceConfiguration = configurator.configure(context, webServer, DEPRECATED_SETTINGS);
            context.getMonitor().warning(
                    format("Deprecated settings group %s is being used for Management API configuration, please switch to the new group %s",
                            DEPRECATED_SETTINGS.apiConfigKey(), "web.http.management"));

        } else {
            webServiceConfiguration = managementApiConfiguration;
        }

        registerCallbackUrl(context, webServiceConfiguration.getPath(), webServiceConfiguration.getPort());

        webService.registerResource(webServiceConfiguration.getContextAlias(), new HttpProvisionerWebhookApiController(transferProcessService));
        webService.registerResource(webServiceConfiguration.getContextAlias(), new AuthenticationRequestFilter(authService));

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
