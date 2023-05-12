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

import static java.util.Objects.requireNonNull;

/**
 * {@link ContractAgreement} to regulate data transfer between two parties.
 */
@JsonDeserialize(builder = ContractAgreement.Builder.class)
public class ContractAgreement {

    private String id;
    private String providerId;
    private String consumerId;
    private long contractSigningDate;
    private String assetId;
    private Policy policy;

    private ContractAgreement() {
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
        return Objects.hash(id, providerId, consumerId, contractSigningDate, assetId, policy);
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
        return contractSigningDate == that.contractSigningDate &&
                Objects.equals(id, that.id) && Objects.equals(providerId, that.providerId) && Objects.equals(consumerId, that.consumerId) &&
                Objects.equals(assetId, that.assetId) && Objects.equals(policy, that.policy);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final ContractAgreement instance;

        private Builder() {
            instance = new ContractAgreement();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.instance.id = id;
            return this;
        }

        public Builder providerId(String providerId) {
            this.instance.providerId = providerId;
            return this;
        }

        public Builder consumerId(String consumerId) {
            this.instance.consumerId = consumerId;
            return this;
        }

        public Builder contractSigningDate(long contractSigningDate) {
            this.instance.contractSigningDate = contractSigningDate;
            return this;
        }

        public Builder assetId(String assetId) {
            this.instance.assetId = assetId;
            return this;
        }

        public Builder policy(Policy policy) {
            this.instance.policy = policy;
            return this;
        }

        public ContractAgreement build() {
            requireNonNull(instance.id, "id cannot be null");
            requireNonNull(instance.providerId, "providerId cannot be null");
            requireNonNull(instance.consumerId, "consumerId cannot be null");
            requireNonNull(instance.assetId, "assetId cannot be null");
            requireNonNull(instance.policy, "policy cannot be null");

            return instance;
        }
    }
}
