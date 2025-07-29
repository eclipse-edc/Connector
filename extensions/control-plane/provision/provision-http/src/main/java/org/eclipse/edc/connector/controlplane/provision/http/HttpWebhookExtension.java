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

import org.eclipse.edc.connector.controlplane.provision.http.webhook.HttpProvisionerWebhookApiController;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.context.ManagementApiUrl;

import java.net.MalformedURLException;
import java.net.URL;

@Provides(HttpProvisionerWebhookUrl.class)
@Deprecated(since = "0.14.0")
public class HttpWebhookExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private ManagementApiUrl managementApiUrl;

    @Override
    public String name() {
        return "DEPRECATED: HttpWebhookExtension";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerCallbackUrl(context);

        webService.registerResource(ApiContext.MANAGEMENT, new HttpProvisionerWebhookApiController(transferProcessService));
    }

    private void registerCallbackUrl(ServiceExtensionContext context) {
        try {
            var url = new URL(managementApiUrl.get() + "/callback");
            context.registerService(HttpProvisionerWebhookUrl.class, () -> url);
        } catch (MalformedURLException e) {
            context.getMonitor().severe("Error creating callback endpoint", e);
            throw new EdcException(e);
        }
    }

}
