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

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

/**
 * Manages contract negotiations on a client or provider. Subtypes define client- and provider-specific operations.
 *
 * All operations are idempotent.
 */
public interface ContractNegotiationManager {

    /**
     * A negotiation was declined by the counter-party represented by the claim token.
     */
    NegotiationResponse declined(ClaimToken token, String negotiationId);

}
