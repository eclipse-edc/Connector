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

package org.eclipse.dataspaceconnector.spi.contract.validation;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

/**
 * Validates {@link ContractOffer}s and {@link ContractAgreement}s.
 */
public interface ContractValidationService {

    /**
     * Validates and sanitizes the contract offer for the consumer represented by the given claims.
     *
     * The original offer must be validated and sanitized to avoid policy and asset injection attacks by malicious consumers.
     */
    @NotNull
    Result<ContractOffer> validate(ClaimToken token, ContractOffer offer);

    /**
     * During the negotiation process, it may be necessary to validate a contract offer against one that is only persisted by the contract negotiation and not known to the ContractDefinitionService.
     */
    @NotNull
    Result<ContractOffer> validate(ClaimToken token, ContractOffer offer, ContractOffer latestOffer);

    /**
     * Validates the contract agreement for the consumer, which must be the contract counter-party.
     */
    boolean validate(ClaimToken token, ContractAgreement agreement);

    /**
     * During the negotiation process, it may be necessary to validate a contract agreement against an offer that is only persisted by the contract negotiation and not known to the ContractDefinitionService.
     */
    boolean validate(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer);
}
