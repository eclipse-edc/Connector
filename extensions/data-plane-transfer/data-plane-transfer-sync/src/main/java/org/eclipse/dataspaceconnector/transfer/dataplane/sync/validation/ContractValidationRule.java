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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.validation;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.common.token.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;

/**
 * Assert that contract still allows access to the data. As of current implementation it only validates the contract end date.
 */
public class ContractValidationRule implements TokenValidationRule {

    private final ContractNegotiationStore contractNegotiationStore;
    private final Clock clock;

    public ContractValidationRule(ContractNegotiationStore contractNegotiationStore, Clock clock) {
        this.contractNegotiationStore = contractNegotiationStore;
        this.clock = clock;
    }

    @Override
    public Result<SignedJWT> checkRule(@NotNull SignedJWT toVerify, @Nullable Map<String, Object> additional) {
        String contractId;
        try {
            contractId = toVerify.getJWTClaimsSet().getStringClaim(CONTRACT_ID);
        } catch (ParseException e) {
            return Result.failure("Failed to parse claims");
        }

        if (contractId == null) {
            return Result.failure(String.format("Missing contract id claim `%s`", CONTRACT_ID));
        }

        var contractAgreement = contractNegotiationStore.findContractAgreement(contractId);
        if (contractAgreement == null) {
            return Result.failure("No contract agreement found for id: " + contractId);
        }

        if (clock.instant().isAfter(Instant.ofEpochSecond(contractAgreement.getContractEndDate()))) {
            return Result.failure("Contract has expired");
        }

        return Result.success(toVerify);
    }
}
