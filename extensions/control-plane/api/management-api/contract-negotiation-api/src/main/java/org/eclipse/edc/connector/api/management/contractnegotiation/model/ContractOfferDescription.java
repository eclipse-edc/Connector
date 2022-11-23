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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.api.model.DurationDto;
import org.eclipse.edc.policy.model.Policy;

import java.util.concurrent.TimeUnit;

public class ContractOfferDescription {

    /**
     * Default validity is set to one year.
     */
    private static final DurationDto DEFAULT_VALIDITY = DurationDto.Builder.newInstance()
            .unit(TimeUnit.DAYS.toString())
            .value(365)
            .build();

    @NotBlank(message = "offerId is mandatory")
    private String offerId;
    @NotBlank(message = "assetId is mandatory")
    private String assetId;
    @NotNull(message = "policy cannot be null")
    private Policy policy;
    @NotNull(message = "duration cannot be null")
    private DurationDto validity = DEFAULT_VALIDITY;

    private ContractOfferDescription() {
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

    public DurationDto getValidity() {
        return validity;
    }

    public static final class Builder {
        private final ContractOfferDescription dto;

        private Builder() {
            dto = new ContractOfferDescription();
        }

        public static ContractOfferDescription.Builder newInstance() {
            return new ContractOfferDescription.Builder();
        }

        public ContractOfferDescription.Builder offerId(String offerId) {
            dto.offerId = offerId;
            return this;
        }

        public ContractOfferDescription.Builder assetId(String assetId) {
            dto.assetId = assetId;
            return this;
        }

        public ContractOfferDescription.Builder policy(Policy policy) {
            dto.policy = policy;
            return this;
        }

        public ContractOfferDescription.Builder validity(DurationDto validity) {
            dto.validity = validity;
            return this;
        }

        public ContractOfferDescription build() {
            return dto;
        }
    }
}
