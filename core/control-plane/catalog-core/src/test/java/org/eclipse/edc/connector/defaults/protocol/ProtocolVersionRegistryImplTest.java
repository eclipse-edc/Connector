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

package org.eclipse.edc.connector.defaults.protocol;

import org.eclipse.edc.connector.controlplane.spi.protocol.ProtocolVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolVersionRegistryImplTest {

    private final ProtocolVersionRegistryImpl registry = new ProtocolVersionRegistryImpl();

    @Test
    void shouldRegisterAndRetrieve() {
        var version = new ProtocolVersion("version", "/path");
        registry.register(version);

        var result = registry.getAll();

        assertThat(result.protocolVersions()).containsOnly(version);
    }

    @Test
    void shouldNotReturnDuplicatedEntries() {
        registry.register(new ProtocolVersion("version", "/path"));
        registry.register(new ProtocolVersion("version", "/path"));

        var result = registry.getAll();

        assertThat(result.protocolVersions()).hasSize(1);
    }
}
