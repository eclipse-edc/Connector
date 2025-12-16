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

import java.util.Map;
import java.util.Objects;

/**
 * Represents a system running on behalf of a dataspace participant.
 * <p>
 * A ParticipantAgent is not the same as a Participant since the former may have claims and attributes that are distinct from the latter. For example, the Acme organization may
 * have connector systems deployed in different geographical regions. While Acme may possess the claim/attribute Partner=GOLD, only the connector running in the EU will have the
 * claim Region=EU.
 * <p>
 * Claims are verifiable claims presented to the current runtime by the ParticipantAgent system, typically as part of a security token or credential store. Attributes are
 * additional values added by the current system based on.
 */
public class ParticipantAgent {

    /**
     * The dataspace participant identity attribute key.
     *
     * @deprecated replaced by {@link #id}.
     */
    @Deprecated(since = "0.14.0")
    public static final String PARTICIPANT_IDENTITY = "edc:identity";

    private final String id;
    private final Map<String, Object> claims;
    private final Map<String, String> attributes;
    
    public ParticipantAgent(String id, Map<String, Object> claims, Map<String, String> attributes) {
        this.id = Objects.requireNonNull(id, "identity cannot be null");
        this.claims = Map.copyOf(claims);
        this.attributes = Map.copyOf(attributes);
    }

    @Deprecated(since = "0.14.0")
    public ParticipantAgent(Map<String, Object> claims, Map<String, String> attributes) {
        this(attributes.get(PARTICIPANT_IDENTITY), claims, attributes);
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
     * Returns the identity of the participant.
     */
    @NotNull
    public String getIdentity() {
        return id;
    }
}
