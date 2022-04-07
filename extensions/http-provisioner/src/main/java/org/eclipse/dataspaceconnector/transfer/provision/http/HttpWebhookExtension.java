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

package org.eclipse.dataspaceconnector.transfer.provision.http;

import org.eclipse.dataspaceconnector.api.auth.AuthenticationRequestFilter;
import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.api.exception.mappers.EdcApiExceptionMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.WebServer;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Hostname;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.transfer.provision.http.webhook.HttpProvisionerWebhookApiController;

import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;

@Provides(HttpProvisionerWebhookUrl.class)
public class HttpWebhookExtension implements ServiceExtension {
    //not an EdcSetting, because it is only the first part of the config path
    public static final String PROVISIONER_WEBHOOK_CONFIG = "web.http.provisioner";
    public static final String PROVISIONER_CONTEXT_ALIAS = "provisioner";

    public static final int DEFUALT_WEBHOOK_PORT = 8383;
    public static final String DEFAULT_PROVISIONER_CONTEXT_PATH = "/api/v1/provisioner";

    @Inject
    private WebServer webServer;

    @Inject
    private WebService webService;

    @Inject
    private AuthenticationService authService;

    @Inject
    private TransferProcessManager transferProcessManager;

    @Inject
    private Hostname hostname;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var alias = PROVISIONER_CONTEXT_ALIAS;
        var path = DEFAULT_PROVISIONER_CONTEXT_PATH;
        var port = DEFUALT_WEBHOOK_PORT;

        var config = context.getConfig(PROVISIONER_WEBHOOK_CONFIG);
        if (config.getEntries().isEmpty()) {
            monitor.warning(format("Settings for [%s] and/or [%s] were not provided. Using default" +
                    " value(s) instead.", PROVISIONER_WEBHOOK_CONFIG + ".path", PROVISIONER_WEBHOOK_CONFIG + ".path"));
            webServer.addPortMapping(alias, port, path);
        } else {
            path = config.getString("path", path);
            port = config.getInteger("port", port);
        }

        registerCallbackUrl(context, path, port);

        monitor.info(format("IDS API will be available at [path=%s], [port=%s].", path, port));


        webService.registerResource(alias, new HttpProvisionerWebhookApiController(transferProcessManager));
        webService.registerResource(alias, new AuthenticationRequestFilter(authService));
        webService.registerResource(alias, new EdcApiExceptionMapper());
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
