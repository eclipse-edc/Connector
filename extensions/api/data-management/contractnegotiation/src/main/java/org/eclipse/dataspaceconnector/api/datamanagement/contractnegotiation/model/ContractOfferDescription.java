/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.eclipse.dataspaceconnector.policy.model.Policy;

public class ContractOfferDescription {
    @NotNull
    private final String offerId;
    @NotNull
    private final String assetId;
    @NotNull
    private final Policy policy;

    @JsonCreator
    public ContractOfferDescription(@JsonProperty("offerId") String offerId,
                                    @JsonProperty("assetId") String assetId,
                                    @JsonProperty("policy") Policy policy) {
        this.offerId = offerId;
        this.assetId = assetId;
        this.policy = policy;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getAssetId() {
        return assetId;
    }

    public Policy getPolicy() {
        return policy;
    }
}