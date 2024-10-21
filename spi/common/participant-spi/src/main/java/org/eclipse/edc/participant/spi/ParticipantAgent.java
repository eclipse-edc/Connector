/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.participant.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents a system running on behalf of a dataspace participant.
 * <p>
 * A ParticipantAgent is not the same as a Participant since the former may have claims and attributes that are distinct from the latter. For example, the Acme organization may
 * have connector systems deployed in different geographical regions. While Acme may possess the claim/attribute Partner=GOLD, only the connector running in the EU will have the
 * claim Region=EU.
 * <p>
 * Claims are verifiable claims presented to the current runtime by the ParticipantAgent system, typically as part of a security token or credential store. Attributes are
 * additional values added by the current system based on.
 * <p>
 * Participant agents must have an {@link #PARTICIPANT_IDENTITY} attribute set. The value of the attribute is determined using a dataspace-specific mechanism, but it must be from
 * a trusted source since the value is used to authorize operations such as contract negotiations and data transfers. For example, the identity value may be based on a claim token
 * or a check against a trusted service. The default behavior is to set the identity using the <code>client_id</code> claim, if present. This behavior can be overridden by using a
 * {@link ParticipantAgentServiceExtension}.
 */
public class ParticipantAgent {

    /**
     * The dataspace participant identity attribute key.
     */
    public static final String PARTICIPANT_IDENTITY = "edc:identity";

    private final Map<String, Object> claims;
    private final Map<String, String> attributes;

    public ParticipantAgent(Map<String, Object> claims, Map<String, String> attributes) {
        this.claims = Map.copyOf(claims);
        this.attributes = Map.copyOf(attributes);
    }

    /**
     * Returns the claims such as verifiable credentials associated with the agent.
     */
    @NotNull
    public Map<String, Object> getClaims() {
        return claims;
    }

    /**
     * Returns additional attributes associated with the agent. Attributes can be added to identify traits or aspects of an agent such as an access group they are part of.
     */
    @NotNull
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Returns the verified identity of the participant or null if one is not available.
     */
    @Nullable
    public String getIdentity() {
        return attributes.get(PARTICIPANT_IDENTITY);
    }
}
