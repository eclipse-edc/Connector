/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.health.LivenessProvider;
import org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(DependencyInjectionExtension.class)
class ObservabilityApiExtensionTest {

    private ObservabilityApiExtension extension;
    private WebService webServiceMock;
    private HealthCheckService healthServiceMock;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        webServiceMock = mock(WebService.class);
        context.registerService(WebService.class, webServiceMock);
        healthServiceMock = mock(HealthCheckService.class);
        context.registerService(HealthCheckService.class, healthServiceMock);
        extension = factory.constructInstance(ObservabilityApiExtension.class);
    }

    @Test
    void initialize() {
        ServiceExtensionContext contextMock = mock(ServiceExtensionContext.class);

        extension.initialize(contextMock);

        verify(webServiceMock).registerResource(isA(ObservabilityApiController.class));
        verify(healthServiceMock).addReadinessProvider(isA(ReadinessProvider.class));
        verify(healthServiceMock).addLivenessProvider(isA(LivenessProvider.class));
        verifyNoMoreInteractions(webServiceMock);
        verifyNoMoreInteractions(healthServiceMock);

        verifyNoMoreInteractions(contextMock);
    }
}