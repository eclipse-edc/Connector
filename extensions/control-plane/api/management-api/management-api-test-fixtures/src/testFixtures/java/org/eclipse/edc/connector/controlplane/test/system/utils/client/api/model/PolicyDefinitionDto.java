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

/**
 * DTO representation of a Policy Definition.
 */
public final class PolicyDefinitionDto extends Typed {
    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String id;
    private final PolicyDto policy;

    public PolicyDefinitionDto(String id,
                               PolicyDto policy) {
        super("PolicyDefinition");
        this.id = id;
        this.policy = policy;
    }

    public PolicyDefinitionDto(PolicyDto policy) {
        this(null, policy);
    }

    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getId() {
        return id;
    }

    public PolicyDto getPolicy() {
        return policy;
    }

}
