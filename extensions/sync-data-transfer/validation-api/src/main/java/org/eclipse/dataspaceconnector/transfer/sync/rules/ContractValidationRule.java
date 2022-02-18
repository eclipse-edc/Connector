/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.sync.rules;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema;
import org.eclipse.dataspaceconnector.token.JwtClaimValidationRule;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.time.Instant;

/**
 * Assert that contract still allows access to the data. As of current implementation it only validates the contract end date.
 */
public class ContractValidationRule implements JwtClaimValidationRule {

    private final ContractNegotiationStore contractNegotiationStore;

    public ContractValidationRule(ContractNegotiationStore contractNegotiationStore) {
        this.contractNegotiationStore = contractNegotiationStore;
    }

    @Override
    public Result<JWTClaimsSet> checkRule(@NotNull JWTClaimsSet toVerify) {
        String contractId;
        try {
            contractId = toVerify.getStringClaim(EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAM);
        } catch (ParseException e) {
            return Result.failure("Failed to parse claims");
        }

        if (contractId == null) {
            return Result.failure("Missing contract id claim `cid`");
        }

        ContractAgreement contractAgreement = contractNegotiationStore.findContractAgreement(contractId);
        if (contractAgreement == null) {
            return Result.failure("No contract agreement found for id: " + contractId);
        }

        // check contract expiration date
        if (Instant.now().isAfter(Instant.ofEpochSecond(contractAgreement.getContractEndDate()))) {
            return Result.failure("Contract has expired");
        }

        return Result.success(toVerify);
    }
}
