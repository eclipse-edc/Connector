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
 * DTO representation of a Contract Request.
 */
public final class ContractRequestDto extends Typed {

    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String id;
    private final String profile;
    private final String counterPartyAddress;
    private final String counterPartyId;
    private final OfferDto policy;

    public ContractRequestDto(String id,
                              String profile,
                              String counterPartyAddress,
                              String counterPartyId,
                              OfferDto policy) {
        super("ContractRequest");
        this.id = id;
        this.profile = profile;
        this.counterPartyAddress = counterPartyAddress;
        this.counterPartyId = counterPartyId;
        this.policy = policy;
    }

    public ContractRequestDto(String profile, String counterPartyAddress, String counterPartyId, OfferDto policy) {
        this(null, profile, counterPartyAddress, counterPartyId, policy);
    }

    public String getId() {
        return id;
    }

    public String getProfile() {
        return profile;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public OfferDto getPolicy() {
        return policy;
    }

}
