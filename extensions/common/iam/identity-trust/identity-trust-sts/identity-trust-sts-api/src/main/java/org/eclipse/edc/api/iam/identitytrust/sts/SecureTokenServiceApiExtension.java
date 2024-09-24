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

import org.eclipse.edc.api.iam.identitytrust.sts.controller.SecureTokenServiceApiController;
import org.eclipse.edc.api.iam.identitytrust.sts.exception.StsTokenExceptionMapper;
import org.eclipse.edc.api.iam.identitytrust.sts.validation.StsTokenRequestValidator;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

@Extension(SecureTokenServiceApiExtension.NAME)
public class SecureTokenServiceApiExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service API";

    @Inject
    private StsAccountService clientService;

    @Inject
    private StsClientTokenGeneratorService tokenService;

    @Inject
    private WebService webService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.STS, new SecureTokenServiceApiController(clientService, tokenService, new StsTokenRequestValidator()));
        webService.registerResource(ApiContext.STS, new StsTokenExceptionMapper());
    }
}
