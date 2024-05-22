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

package org.eclipse.edc.connector.controlplane.provision.http;

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.controlplane.provision.http.webhook.HttpProvisionerWebhookApiController;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.net.MalformedURLException;
import java.net.URL;

@Provides(HttpProvisionerWebhookUrl.class)
public class HttpWebhookExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private Hostname hostname;

    @Inject
    private ManagementApiConfiguration managementApiConfiguration;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerCallbackUrl(context, managementApiConfiguration.getPath(), managementApiConfiguration.getPort());

        webService.registerResource(managementApiConfiguration.getContextAlias(), new HttpProvisionerWebhookApiController(transferProcessService));
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
