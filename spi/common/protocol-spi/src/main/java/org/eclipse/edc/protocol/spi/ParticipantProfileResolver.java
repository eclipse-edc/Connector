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

package org.eclipse.edc.protocol.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the {@link DataspaceProfileContext}s a given participant context is associated with.
 *
 * <p>Profile association is stored as a per-participant configuration entry under
 * {@link #PROFILES_CONFIG_KEY} (a comma-separated list of profile ids). The resolver looks each
 * id up in the {@link DataspaceProfileContextRegistry}; ids that don't resolve to a registered
 * profile are skipped silently.
 */
@ExtensionPoint
public interface ParticipantProfileResolver {

    /**
     * Configuration key under {@code ParticipantContextConfig} that holds the comma-separated list
     * of profile ids associated with a participant.
     */
    String PROFILES_CONFIG_KEY = "edc.dsp.profiles";

    /**
     * Returns all profiles the participant is associated with. Profile ids configured but not
     * registered in the registry are skipped (not an error).
     *
     * @param participantContextId the participant context identifier
     * @return the resolved profiles, in the order they appear in the config; empty if none
     */
    List<DataspaceProfileContext> resolveAll(String participantContextId);

    /**
     * Returns the profile if and only if the participant is associated with it AND it is
     * registered in the registry. Use this to validate inbound DSP requests at
     * {@code /{participantContextId}/{profileId}/...}.
     *
     * @param participantContextId the participant context identifier
     * @param profileId            the profile id
     * @return the resolved profile, or empty if the participant is not associated with it
     */
    Optional<DataspaceProfileContext> resolve(String participantContextId, String profileId);
}
