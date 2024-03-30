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

package org.eclipse.edc.connector.controlplane.defaults.protocol;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersion;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;

import java.util.ArrayList;
import java.util.List;

public class ProtocolVersionRegistryImpl implements ProtocolVersionRegistry {

    private final List<ProtocolVersion> versions = new ArrayList<>();

    @Override
    public void register(ProtocolVersion protocolVersion) {
        versions.add(protocolVersion);
    }

    @Override
    public ProtocolVersions getAll() {
        return new ProtocolVersions(versions.stream().distinct().toList());
    }
}
