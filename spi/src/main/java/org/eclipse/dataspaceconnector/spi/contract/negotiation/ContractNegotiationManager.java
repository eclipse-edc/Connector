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
package org.eclipse.dataspaceconnector.spi.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

/**
 * Manages contract negotiations on a consumer or provider. Subtypes define consumer- and provider-specific operations.
 *
 * All operations are idempotent.
 */
public interface ContractNegotiationManager {

    String FEATURE = "edc:core:contract";

    /**
     * A negotiation was declined by the counter-party represented by the claim token.
     */
    NegotiationResult declined(ClaimToken token, String negotiationId);

}
