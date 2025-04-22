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
 *       Cofinity-X - unauthenticated DSP version endpoint
 *
 */

package org.eclipse.edc.connector.controlplane.services.protocol;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VersionProtocolServiceImplTest {

    private final ProtocolVersionRegistry registry = mock();
    private final VersionProtocolServiceImpl service = new VersionProtocolServiceImpl(registry);

    @Test
    void shouldReturnAllProtocolVersions() {
        var protocolVersions = new ProtocolVersions(Collections.emptyList());
        when(registry.getAll()).thenReturn(protocolVersions);

        var result = service.getAll();

        assertThat(result).isSucceeded().isSameAs(protocolVersions);
    }
}
