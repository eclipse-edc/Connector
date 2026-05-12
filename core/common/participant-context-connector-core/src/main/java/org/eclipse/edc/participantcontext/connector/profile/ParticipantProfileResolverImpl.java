/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.participantcontext.connector.profile;

import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ParticipantProfileResolverImpl implements ParticipantProfileResolver {

    private final ParticipantContextConfig participantContextConfig;
    private final DataspaceProfileContextRegistry profileRegistry;
    private final Boolean dspEnableAllProfiles;

    public ParticipantProfileResolverImpl(ParticipantContextConfig participantContextConfig,
                                          DataspaceProfileContextRegistry profileRegistry, Boolean dspEnableAllProfiles) {
        this.participantContextConfig = participantContextConfig;
        this.profileRegistry = profileRegistry;
        this.dspEnableAllProfiles = dspEnableAllProfiles;
    }

    private static Set<String> parse(String csv) {
        return Stream.of(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<DataspaceProfileContext> resolveAll(String participantContextId) {
        if (dspEnableAllProfiles) {
            return profileRegistry.getProfiles();
        }
        return parse(readRaw(participantContextId)).stream()
                .map(profileRegistry::getProfile)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<DataspaceProfileContext> resolve(String participantContextId, String profileId) {

        if (dspEnableAllProfiles) {
            var profile = profileRegistry.getProfile(profileId);
            return Optional.ofNullable(profile);
        }
        var configured = parse(readRaw(participantContextId));
        if (!configured.contains(profileId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(profileRegistry.getProfile(profileId));
    }

    private String readRaw(String participantContextId) {
        return participantContextConfig.getString(participantContextId, PROFILES_CONFIG_KEY, "");
    }
}
