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
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.edc.protocol.ids.api.configuration;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

/**
 * Provides configuration information for IDS API endpoints to other extensions.
 */
@Provides(IdsApiConfiguration.class)
@Extension(value = IdsApiConfigurationExtension.NAME)
public class IdsApiConfigurationExtension implements ServiceExtension {


    @Setting
    public static final String IDS_WEBHOOK_ADDRESS = "ids.webhook.address";
    public static final String DEFAULT_IDS_WEBHOOK_ADDRESS = "http://localhost";
    public static final String NAME = "IDS API Configuration";

    public static final String WEBSERVICE_NAME = "IDS API";


    public static final String IDS_API_CONFIG = "web.http.ids";
    public static final String IDS_API_CONTEXT_ALIAS = "ids";

    public static final int DEFAULT_IDS_PORT = 8282;
    public static final String DEFAULT_IDS_API_PATH = "/api/v1/ids";


    @Inject
    private WebServer webServer;


    @Inject
    private WebServiceConfigurer configurator;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var settings = WebServiceSettings.Builder.newInstance()
                .apiConfigKey(IDS_API_CONFIG)
                .contextAlias(IDS_API_CONTEXT_ALIAS)
                .defaultPath(DEFAULT_IDS_API_PATH)
                .defaultPort(DEFAULT_IDS_PORT)
                .name(WEBSERVICE_NAME)
                .build();
        var config = configurator.configure(context, webServer, settings);

        var path = config.getPath();
        var webhookPath = path + (path.endsWith("/") ? "data" : "/data");
        var idsWebhookAddress = context.getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS) + webhookPath;

        context.registerService(IdsApiConfiguration.class, new IdsApiConfiguration(config.getContextAlias(), idsWebhookAddress));
    }

}
