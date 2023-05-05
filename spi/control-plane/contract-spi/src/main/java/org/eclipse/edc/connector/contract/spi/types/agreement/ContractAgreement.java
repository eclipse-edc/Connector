/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - refactor
 *
 */

package org.eclipse.edc.connector.contract.spi.types.agreement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * {@link ContractAgreement} to regulate data transfer between two parties.
 */
@JsonDeserialize(builder = ContractAgreement.Builder.class)
public class ContractAgreement {

    private final String id;
    private final String providerId;
    private final String consumerId;
    private final long contractSigningDate;
    private final long contractStartDate;
    private final long contractEndDate;
    private final String assetId;
    private final Policy policy;

    private ContractAgreement(@NotNull String id,
                              @NotNull String providerId,
                              @NotNull String consumerId,
                              long contractSigningDate,
                              long contractStartDate,
                              long contractEndDate,
                              @NotNull Policy policy,
                              @NotNull String assetId) {
        this.id = Objects.requireNonNull(id);
        this.providerId = Objects.requireNonNull(providerId);
        this.consumerId = Objects.requireNonNull(consumerId);
        this.contractSigningDate = contractSigningDate;
        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;
        this.assetId = Objects.requireNonNull(assetId);
        this.policy = Objects.requireNonNull(policy);
    }

    /**
     * Unique identifier of the {@link ContractAgreement}.
     *
     * @return contract id
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * The id of the data providing participant.
     * Please note that id should be taken from the corresponding data ecosystem.
     *
     * @return provider id
     */
    @NotNull
    public String getProviderId() {
        return providerId;
    }

    /**
     * The id of the data consuming participant.
     * Please note that id should be taken from the corresponding contract ecosystem.
     *
     * @return consumer id
     */
    @NotNull
    public String getConsumerId() {
        return consumerId;
    }

    /**
     * The date when the {@link ContractAgreement} has been signed. <br>
     * Numeric value representing the number of seconds from
     * 1970-01-01T00:00:00Z UTC until the specified UTC date/time.
     *
     * @return contract signing date
     */
    public long getContractSigningDate() {
        return contractSigningDate;
    }

    /**
     * The date from when the {@link ContractAgreement} is valid. <br>
     * Numeric value representing the number of seconds from
     * 1970-01-01T00:00:00Z UTC until the specified UTC date/time.
     *
     * @return contract start date
     */
    public long getContractStartDate() {
        return contractStartDate;
    }

    /**
     * The date until the {@link ContractAgreement} remains valid. <br>
     * Numeric value representing the number of seconds from
     * 1970-01-01T00:00:00Z UTC until the specified UTC date/time.
     *
     * @return contract end date
     */
    public long getContractEndDate() {
        return contractEndDate;
    }

    /**
     * The ID of the Asset that is covered by the {@link ContractAgreement}.
     *
     * @return assetId
     */
    @NotNull
    public String getAssetId() {
        return assetId;
    }

    /**
     * The id of the policy that describes how the {@link Asset} of this contract may be used by the consumer.
     *
     * @return policy
     */
    public Policy getPolicy() {
        return policy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, providerId, consumerId, contractSigningDate, contractStartDate, contractEndDate, assetId, policy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContractAgreement that = (ContractAgreement) o;
        return contractSigningDate == that.contractSigningDate && contractStartDate == that.contractStartDate && contractEndDate == that.contractEndDate &&
                Objects.equals(id, that.id) && Objects.equals(providerId, that.providerId) && Objects.equals(consumerId, that.consumerId) &&
                Objects.equals(assetId, that.assetId) && Objects.equals(policy, that.policy);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private String id;
        private String providerId;
        private String consumerId;
        private long contractSigningDate;
        private long contractStartDate;
        private long contractEndDate;
        private String assetId;
        private Policy policy;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder consumerId(String consumerId) {
            this.consumerId = consumerId;
            return this;
        }

        public Builder contractSigningDate(long contractSigningDate) {
            this.contractSigningDate = contractSigningDate;
            return this;
        }

        public Builder contractStartDate(long contractStartDate) {
            this.contractStartDate = contractStartDate;
            return this;
        }

        public Builder contractEndDate(long contractEndDate) {
            this.contractEndDate = contractEndDate;
            return this;
        }

        public Builder assetId(String assetId) {
            this.assetId = assetId;
            return this;
        }

        public Builder policy(Policy policy) {
            this.policy = policy;
            return this;
        }

        public ContractAgreement build() {
            return new ContractAgreement(id, providerId, consumerId, contractSigningDate, contractStartDate, contractEndDate, policy, assetId);
        }
    }
}
