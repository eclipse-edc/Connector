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

package org.eclipse.edc.iam.identitytrust.core;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class IatpDefaultServicesExtensionTest {

    @Test
    void verify_defaultService(ServiceExtensionContext context, ObjectFactory factory) {
        Monitor mockedMonitor = mock();
        context.registerService(Monitor.class, mockedMonitor);
        var ext = factory.constructInstance(IatpDefaultServicesExtension.class);
        var sts = ext.createDefaultTokenService(context);

        assertThat(sts).isInstanceOf(EmbeddedSecureTokenService.class);
        verify(mockedMonitor).info(anyString());
    }

    @Test
    void verify_defaultServiceWithWarning(ServiceExtensionContext context, ObjectFactory factory) {
        Monitor mockedMonitor = mock();
        context.registerService(Monitor.class, mockedMonitor);
        when(context.getSetting(eq("edc.oauth.token.url"), any())).thenReturn("https://some.url");

        var ext = factory.constructInstance(IatpDefaultServicesExtension.class);
        var sts = ext.createDefaultTokenService(context);

        verify(mockedMonitor).info(anyString());
        verify(mockedMonitor).warning(anyString());
    }
}