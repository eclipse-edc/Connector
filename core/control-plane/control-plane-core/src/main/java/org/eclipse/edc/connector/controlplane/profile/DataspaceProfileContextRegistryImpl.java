/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.profile;

import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantIdExtractionFunction;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.protocol.spi.ProtocolWebhook;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DataspaceProfileContextRegistryImpl implements DataspaceProfileContextRegistry {

    private final List<DataspaceProfileContext> defaultProfiles = new ArrayList<>();
    private final List<DataspaceProfileContext> standardProfiles = new ArrayList<>();

    @Override
    public void registerDefault(DataspaceProfileContext profileContext) {
        defaultProfiles.add(profileContext);
    }

    @Override
    public void register(DataspaceProfileContext context) {
        standardProfiles.add(context);
    }

    @Override
    public ProtocolVersions getProtocolVersions() {
        var versions = profiles().stream().map(DataspaceProfileContext::protocolVersion).distinct().toList();

        return new ProtocolVersions(versions);
    }

    @Override
    public @Nullable ProtocolWebhook getWebhook(String protocol) {
        return profiles().stream().filter(it -> it.name().equals(protocol))
                .map(DataspaceProfileContext::webhook).findAny().orElse(null);
    }

    @Override
    public @Nullable ProtocolVersion getProtocolVersion(String protocol) {
        return profiles().stream().filter(it -> it.name().equals(protocol))
                .map(DataspaceProfileContext::protocolVersion).findAny().orElse(null);
    }
    
    @Override
    public @Nullable String getParticipantId(String protocol) {
        return profiles().stream()
                .filter(it -> it.name().equals(protocol))
                .map(DataspaceProfileContext::participantId)
                .findAny()
                .orElse(null);
    }
    
    
    @Override
    public @Nullable ParticipantIdExtractionFunction getIdExtractionFunction(String protocol) {
        return profiles().stream()
                .filter(it -> it.name().equals(protocol))
                .map(DataspaceProfileContext::idExtractionFunction)
                .findAny()
                .orElse(null);
    }
    
    private List<DataspaceProfileContext> profiles() {
        return standardProfiles.isEmpty() ? defaultProfiles : standardProfiles;
    }
}
