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
 *       Fraunhofer Institute for Software and Systems Engineering - extended parameter definition
 *
 */

package org.eclipse.edc.connector.contract.spi.negotiation;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.response.StatusResult;

/**
 * Manages contract negotiations on the provider.
 * <p>
 * All operations are idempotent.
 */

@ExtensionPoint
public interface ProviderContractNegotiationManager extends ContractNegotiationManager {

    /**
     * A contract negotiation has been requested by the consumer represented with the given claims.
     */
    StatusResult<ContractNegotiation> requested(ClaimToken token, ContractOfferRequest request);

    /**
     * The negotiation has been verified by the consumer.
     */
    StatusResult<ContractNegotiation> verified(ClaimToken token, String negotiationId);
}
