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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.dataspaceconnector.policy.model.Policy;

import java.util.UUID;

public class TestFunctions {
    public static ContractOfferDescription createOffer(String offerId, String assetId, String policyId) {
        return new ContractOfferDescription(offerId, assetId, policyId, null);
    }

    public static ContractOfferDescription createOffer(Policy policy) {
        return new ContractOfferDescription(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null, policy);
    }

    public static ContractOfferDescription createOffer(String offerId) {
        return createOffer(offerId, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static ContractOfferDescription createOffer() {
        return createOffer(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }
}