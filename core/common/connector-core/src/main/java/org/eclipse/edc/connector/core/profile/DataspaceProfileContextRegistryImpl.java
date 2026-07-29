/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.profile;


import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DataspaceProfileContextRegistryImpl implements DataspaceProfileContextRegistry {

    private final Map<String, DataspaceProfileContext> defaultProfiles = new LinkedHashMap<>();
    private final Map<String, DataspaceProfileContext> standardProfiles = new ConcurrentHashMap<>();
    private final List<Consumer<DataspaceProfileContext>> callbacks = new ArrayList<>();

    @Override
    public void registerDefault(DataspaceProfileContext profileContext) {
        defaultProfiles.put(profileContext.name(), profileContext);
        notifyCallbacks(profileContext);
    }

    @Override
    public void register(DataspaceProfileContext context) {
        var prev = standardProfiles.put(context.name(), context);
        // notify callbacks only if the profile was not already registered
        if (prev == null) {
            notifyCallbacks(context);
        }
    }

    @Override
    public void deregister(String profileId) {
        standardProfiles.remove(profileId);
    }

    @Override
    public void addRegistrationCallback(Consumer<DataspaceProfileContext> callback) {
        callbacks.add(callback);
        Stream.concat(defaultProfiles.values().stream(), standardProfiles.values().stream()).forEach(callback);
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
    public List<DataspaceProfileContext> getProfiles() {
        return new ArrayList<>(activeProfiles().values());
    }

    @Override
    public @Nullable DataspaceProfileContext getProfile(String profileId) {
        return activeProfiles().get(profileId);
    }

    private Map<String, DataspaceProfileContext> activeProfiles() {
        return standardProfiles.isEmpty() ? defaultProfiles : standardProfiles;
    }

}
