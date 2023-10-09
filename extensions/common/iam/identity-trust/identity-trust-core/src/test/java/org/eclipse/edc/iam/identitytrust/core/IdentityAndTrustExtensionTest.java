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

import org.eclipse.edc.iam.identitytrust.service.IdentityAndTrustService;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class IdentityAndTrustExtensionTest {

    private IdentityAndTrustExtension extension;
    private ServiceExtensionContext spiedContext;

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        spiedContext = spy(context);
        spiedContext.registerService(SecureTokenService.class, mock());
    }

    @Test
    void verifyCorrectService(IdentityAndTrustExtension extension, ServiceExtensionContext context) {
        var configMock = mock(Config.class);
        when(configMock.getString(eq(IdentityAndTrustExtension.ISSUER_DID_PROPERTY))).thenReturn("did:web:test");
        when(spiedContext.getConfig()).thenReturn(configMock);

        var is = extension.createIdentityService(spiedContext);

        assertThat(is).isInstanceOf(IdentityAndTrustService.class);
        verify(configMock).getString(eq(IdentityAndTrustExtension.ISSUER_DID_PROPERTY));
    }
}