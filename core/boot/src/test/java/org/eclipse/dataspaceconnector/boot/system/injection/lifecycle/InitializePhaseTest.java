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

package org.eclipse.dataspaceconnector.boot.system.injection.lifecycle;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class InitializePhaseTest extends PhaseTest {

    @Test
    void initialize() {
        var ip = new InitializePhase(injector, container, context, monitor);

        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        when(container.validate(isA(ServiceExtensionContext.class))).thenReturn(Result.success());

        ip.initialize();

        verify(monitor).info(anyString());
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void initialize_validationFails() {
        var ip = new InitializePhase(injector, container, context, monitor);

        when(container.getInjectionTarget()).thenReturn(mock(ServiceExtension.class));
        when(container.validate(isA(ServiceExtensionContext.class))).thenReturn(Result.failure("test-failure"));

        ip.initialize();

        verify(monitor).warning(startsWith("There were missing service registrations in extension"));
        verify(monitor).info(anyString());
        verifyNoMoreInteractions(monitor);
    }
}