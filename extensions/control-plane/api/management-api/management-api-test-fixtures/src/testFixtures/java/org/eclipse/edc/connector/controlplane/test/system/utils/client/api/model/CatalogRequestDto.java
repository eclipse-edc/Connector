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

import java.util.List;

/**
 * DTO representation of a Catalog Request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CatalogRequestDto extends Typed {
    private final String profile;
    private final String counterPartyAddress;
    private final String counterPartyId;
    private final List<String> additionalScopes;

    public CatalogRequestDto(String profile,
                             String counterPartyAddress,
                             String counterPartyId) {
        this(profile, counterPartyAddress, counterPartyId, null);
    }

    public CatalogRequestDto(String profile,
                             String counterPartyAddress,
                             String counterPartyId,
                             List<String> additionalScopes) {
        super("CatalogRequest");
        this.profile = profile;
        this.counterPartyAddress = counterPartyAddress;
        this.counterPartyId = counterPartyId;
        this.additionalScopes = additionalScopes;
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

    public List<String> getAdditionalScopes() {
        return additionalScopes;
    }

}
