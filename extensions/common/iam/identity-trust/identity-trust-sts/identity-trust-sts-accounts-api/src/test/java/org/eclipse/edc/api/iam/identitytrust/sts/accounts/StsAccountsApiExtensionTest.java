/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.iam.identitytrust.sts.accounts;

import org.eclipse.edc.api.iam.identitytrust.sts.accounts.controller.StsAccountsApiController;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.web.spi.configuration.ApiContext.STS_ACCOUNTS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class StsAccountsApiExtensionTest {

    private final WebService webService = mock();
    private final Vault vault = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(Vault.class, vault);

        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(emptyMap()));
    }

    @Test
    void initialize(StsAccountsApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(eq(STS_ACCOUNTS), isA(StsAccountsApiController.class));
    }

}
