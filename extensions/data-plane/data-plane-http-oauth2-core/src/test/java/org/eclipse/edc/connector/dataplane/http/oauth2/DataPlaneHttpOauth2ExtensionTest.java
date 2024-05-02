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

package org.eclipse.edc.connector.dataplane.http.oauth2;

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneHttpOauth2ExtensionTest {

    private final HttpRequestParamsProvider paramsProvider = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(HttpRequestParamsProvider.class, paramsProvider);
    }

    @Test
    void verifyRegisterKafkaSource(DataPlaneHttpOauth2Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(paramsProvider).registerSourceDecorator(isA(Oauth2HttpRequestParamsDecorator.class));
        verify(paramsProvider, never()).registerSinkDecorator(isA(Oauth2HttpRequestParamsDecorator.class));
    }

}
