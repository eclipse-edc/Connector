/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.api.iam.identitytrust.sts;

import org.eclipse.edc.api.iam.identitytrust.sts.configuration.StsApiConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

@Extension(value = StsApiConfigurationExtension.NAME)
@Provides({ StsApiConfiguration.class })
public class StsApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service API configuration";
    public static final String STS_CONTEXT_ALIAS = "sts";
    private static final String WEB_SERVICE_NAME = "STS API";
    private static final int DEFAULT_STS_API_PORT = 9292;
    private static final String DEFAULT_STS_API_CONTEXT_PATH = "/api/v1/sts";

    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + STS_CONTEXT_ALIAS)
            .contextAlias(STS_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_STS_API_CONTEXT_PATH)
            .defaultPort(DEFAULT_STS_API_PORT)
            .useDefaultContext(true)
            .name(WEB_SERVICE_NAME)
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
        var config = configurator.configure(context, webServer, SETTINGS);
        context.registerService(StsApiConfiguration.class, new StsApiConfiguration(config));
    }
}
