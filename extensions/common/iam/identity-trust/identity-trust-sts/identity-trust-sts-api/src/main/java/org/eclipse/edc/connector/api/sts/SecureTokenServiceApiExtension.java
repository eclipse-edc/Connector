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

package org.eclipse.edc.connector.api.sts;

import org.eclipse.edc.connector.api.sts.configuration.StsApiConfiguration;
import org.eclipse.edc.connector.api.sts.controller.SecureTokenServiceApiController;
import org.eclipse.edc.connector.api.sts.exception.StsTokenExceptionMapper;
import org.eclipse.edc.connector.api.sts.validation.StsTokenRequestValidator;
import org.eclipse.edc.iam.identitytrust.sts.service.StsClientService;
import org.eclipse.edc.iam.identitytrust.sts.service.StsClientTokenGeneratorService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

@Extension(SecureTokenServiceApiExtension.NAME)
public class SecureTokenServiceApiExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service API";

    @Inject
    private StsApiConfiguration stsApiConfiguration;

    @Inject
    private StsClientService clientService;

    @Inject
    private StsClientTokenGeneratorService tokenService;

    @Inject
    private Monitor monitor;

    @Inject
    private WebService webService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(stsApiConfiguration.getContextAlias(), new SecureTokenServiceApiController(clientService, tokenService, new StsTokenRequestValidator()));
        webService.registerResource(stsApiConfiguration.getContextAlias(), new StsTokenExceptionMapper());
    }
}
