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

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO representation of a Participant Context.
 */
public final class ParticipantContextDto extends Typed {
    @JsonProperty("@id")
    private final String id;
    private final String identity;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String state;
    private final Map<String, Object> properties;

    public ParticipantContextDto(String id, String identity,
                                 String state,
                                 Map<String, Object> properties) {
        super("ParticipantContext");
        this.id = id;
        this.identity = identity;
        this.state = state;
        this.properties = properties;
    }

    public ParticipantContextDto(String participantContextId, String identity) {
        this(participantContextId, identity, null, Map.of());
    }

    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    public String getIdentity() {
        return identity;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getState() {
        return state;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

}
