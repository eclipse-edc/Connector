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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DataspaceProfileContextRegistryImpl implements DataspaceProfileContextRegistry {

    private final List<DataspaceProfileContext> defaultProfiles = new ArrayList<>();
    private final List<DataspaceProfileContext> standardProfiles = new ArrayList<>();
    private final List<Consumer<DataspaceProfileContext>> callbacks = new ArrayList<>();

    @Override
    public void registerDefault(DataspaceProfileContext profileContext) {
        defaultProfiles.add(profileContext);
        notifyCallbacks(profileContext);
    }

    @Override
    public void register(DataspaceProfileContext context) {
        standardProfiles.add(context);
        notifyCallbacks(context);
    }

    @Override
    public void addRegistrationCallback(Consumer<DataspaceProfileContext> callback) {
        callbacks.add(callback);
        Stream.concat(defaultProfiles.stream(), standardProfiles.stream()).forEach(callback);
    }

    private void notifyCallbacks(DataspaceProfileContext profile) {
        callbacks.forEach(cb -> cb.accept(profile));
    }

    @Override
    public ProtocolVersions getProtocolVersions() {
        var versions = getProfiles().stream().map(DataspaceProfileContext::protocolVersion).distinct().toList();

        return new ProtocolVersions(versions);
    }

    @Override
    public @Nullable ProtocolVersion getProtocolVersion(String protocol) {
        var profile = getProfile(protocol);
        return profile == null ? null : profile.protocolVersion();
    }

    @Override
    public @Nullable ParticipantIdExtractionFunction getIdExtractionFunction(String protocol) {
        var profile = getProfile(protocol);
        return profile == null ? null : profile.idExtractionFunction();
    }

    @Override
    public List<DataspaceProfileContext> getProfiles() {
        return standardProfiles.isEmpty() ? defaultProfiles : standardProfiles;
    }

    @Override
    public @Nullable DataspaceProfileContext getProfile(String profileId) {
        return getProfiles().stream()
                .filter(it -> it.name().equals(profileId))
                .findAny()
                .orElse(null);
    }

}
