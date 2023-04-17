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

import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import static java.lang.String.format;

/**
 * Provides configuration information for IDS API endpoints to other extensions.
 */
@Provides({ IdsApiConfiguration.class, DataService.class })
@Extension(value = IdsApiConfigurationExtension.NAME)
public class IdsApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "IDS API Configuration";

    private static final String DEFAULT_IDS_WEBHOOK_ADDRESS = "http://localhost";

    @Setting(value = "The address exposed by the connector to receive ids messages", defaultValue = DEFAULT_IDS_WEBHOOK_ADDRESS)
    public static final String IDS_WEBHOOK_ADDRESS = "ids.webhook.address";

    private static final int DEFAULT_PROTOCOL_PORT = 8282;
    private static final String DEFAULT_PROTOCOL_API_PATH = "/api/v1/ids";

    /**
     * This deprecation is used to permit a smoother transition from the deprecated `web.http.ids` config group to the
     * current `web.http.protocol`
     *
     * @deprecated "web.http.protocol" config should be used instead of "web.http.ids"
     */
    @Deprecated(since = "milestone8")
    public static final WebServiceSettings DEPRECATED_SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http.ids")
            .contextAlias("ids")
            .defaultPath(DEFAULT_PROTOCOL_API_PATH)
            .defaultPort(DEFAULT_PROTOCOL_PORT)
            .name("IDS API")
            .build();

    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http.protocol")
            .contextAlias("protocol")
            .defaultPath(DEFAULT_PROTOCOL_API_PATH)
            .defaultPort(DEFAULT_PROTOCOL_PORT)
            .name("Protocol API")
            .build();

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
        WebServiceSettings settings;
        if (context.getConfig().hasPath(DEPRECATED_SETTINGS.apiConfigKey())) {
            settings = DEPRECATED_SETTINGS;
            context.getMonitor().warning(
                    format("Deprecated settings group %s is being used for Protocol API configuration, please switch to the new group %s",
                            settings.apiConfigKey(), SETTINGS.apiConfigKey()));
        } else {
            settings = SETTINGS;
        }

        var config = configurator.configure(context, webServer, settings);

        var path = config.getPath();
        var webhookPath = path + (path.endsWith("/") ? "data" : "/data");
        var idsWebhookAddress = context.getSetting(IDS_WEBHOOK_ADDRESS, DEFAULT_IDS_WEBHOOK_ADDRESS) + webhookPath;

        context.registerService(IdsApiConfiguration.class, new IdsApiConfiguration(config.getContextAlias(), idsWebhookAddress));

        var dataService = DataService.Builder.newInstance()
                .terms("connector")
                .endpointUrl(idsWebhookAddress)
                .build();
        context.registerService(DataService.class, dataService);
    }

}
