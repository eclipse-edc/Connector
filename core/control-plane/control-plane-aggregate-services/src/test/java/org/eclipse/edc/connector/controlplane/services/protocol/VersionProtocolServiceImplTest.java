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

package org.eclipse.edc.connector.controlplane.services.protocol;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.UNAUTHORIZED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VersionProtocolServiceImplTest {

    private final ProtocolVersionRegistry registry = mock();
    private final ProtocolTokenValidator tokenValidator = mock();
    private final VersionProtocolServiceImpl service = new VersionProtocolServiceImpl(registry, tokenValidator);

    @Test
    void shouldReturnAllProtocolVersions() {
        when(tokenValidator.verify(any(), any())).thenReturn(ServiceResult.success());
        var protocolVersions = new ProtocolVersions(Collections.emptyList());
        when(registry.getAll()).thenReturn(protocolVersions);

        var result = service.getAll(TokenRepresentation.Builder.newInstance().build());

        assertThat(result).isSucceeded().isSameAs(protocolVersions);
    }

    @Test
    void shouldReturnUnauthorized_whenTokenIsNotValid() {
        when(tokenValidator.verify(any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));

        var result = service.getAll(TokenRepresentation.Builder.newInstance().build());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
        verifyNoInteractions(registry);
    }
}
