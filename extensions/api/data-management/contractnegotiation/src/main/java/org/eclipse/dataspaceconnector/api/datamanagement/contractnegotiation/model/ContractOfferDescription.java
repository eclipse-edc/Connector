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
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContractOfferDescription {
    private final String offerId;
    private final String assetId;
    private final String policyId;

    @JsonCreator
    public ContractOfferDescription(@JsonProperty("offerId") String offerId,
                                    @JsonProperty("assetId") String assetId,
                                    @JsonProperty("policyId") String policyId) {
        this.offerId = offerId;
        this.assetId = assetId;
        this.policyId = policyId;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getPolicyId() {
        return policyId;
    }
}