/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.contract.offer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A contract offer is exchanged between two participant agents. It describes the which assets the consumer may use, and the rules and policies that apply to each asset.
 */
@JsonDeserialize(builder = ContractOffer.Builder.class)
public class ContractOffer {
    private String id;

    /**
     * The policy that describes the usage conditions of the assets
     * Must be mutually exclusive with {@link ContractOffer#getPolicyId()}
     */
    private Policy policy;

    /**
     * The offered asset
     * Must be mutually exclusive with {@link ContractOffer#getAssetId()} ()}
     */
    private Asset asset;
    /**
     * Refers to the policy which should be involved in the negotiation. This is only used when <em>initiating</em>
     * a negotiation and indicates, that an existing policy is to be used.
     * Must be mutually exclusive with {@link ContractOffer#getPolicy()}
     */
    private String policyId;
    /**
     * Refers to the asset that is offered. Note that this is only to be used during the actual negotiation and cannot be
     * used in the initial offer from the provider to the consumer.
     * Must be mutually exclusive with {@link ContractOffer#getAsset()}
     */
    private String assetId;
    /**
     * The participant who provides the offered data
     */
    private URI provider;
    /**
     * The participant consuming the offered data
     */
    private URI consumer;
    /**
     * Timestamp defining the start time when the offer becomes effective
     */
    private ZonedDateTime offerStart;
    /**
     * Timestamp defining the end date when the offer becomes ineffective
     */
    private ZonedDateTime offerEnd;
    /**
     * Timestamp defining the start date when the contract becomes effective
     */
    private ZonedDateTime contractStart;
    /**
     * Timestamp defining the end date when the contract becomes terminated
     */
    private ZonedDateTime contractEnd;

    @Nullable
    public String getPolicyId() {
        return policyId;
    }

    @Nullable
    public String getAssetId() {
        return assetId;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public URI getProvider() {
        return provider;
    }

    @Nullable
    public URI getConsumer() {
        return consumer;
    }

    @Nullable
    public ZonedDateTime getOfferStart() {
        return offerStart;
    }

    @Nullable
    public ZonedDateTime getOfferEnd() {
        return offerEnd;
    }

    @Nullable
    public ZonedDateTime getContractStart() {
        return contractStart;
    }

    @Nullable
    public ZonedDateTime getContractEnd() {
        return contractEnd;
    }

    @NotNull
    public Asset getAsset() {
        return asset;
    }

    @Nullable
    public Policy getPolicy() {
        return policy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContractOffer that = (ContractOffer) o;
        return Objects.equals(id, that.id) && Objects.equals(policy, that.policy) && Objects.equals(asset, that.asset) && Objects.equals(provider, that.provider) &&
                Objects.equals(consumer, that.consumer) && Objects.equals(offerStart, that.offerStart) && Objects.equals(offerEnd, that.offerEnd) &&
                Objects.equals(contractStart, that.contractStart) && Objects.equals(contractEnd, that.contractEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, policy, asset, provider, consumer, offerStart, offerEnd, contractStart, contractEnd);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private Asset asset;
        private Policy policy;
        private String id;
        private URI provider;
        private URI consumer;
        private ZonedDateTime offerStart;
        private ZonedDateTime offerEnd;
        private ZonedDateTime contractStart;
        private ZonedDateTime contractEnd;
        private String assetId;
        private String policyId;

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

        public Builder provider(URI provider) {
            this.provider = provider;
            return this;
        }

        public Builder consumer(URI consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder asset(Asset asset) {
            this.asset = asset;
            return this;
        }

        public Builder offerStart(ZonedDateTime date) {
            offerStart = date;
            return this;
        }

        public Builder offerEnd(ZonedDateTime date) {
            offerEnd = date;
            return this;
        }

        public Builder contractStart(ZonedDateTime date) {
            contractStart = date;
            return this;
        }

        public Builder contractEnd(ZonedDateTime date) {
            contractEnd = date;
            return this;
        }

        public Builder policy(Policy policy) {
            this.policy = policy;
            return this;
        }

        public Builder policyId(String policyId) {
            this.policyId = policyId;
            return this;
        }

        public Builder assetId(String assetId) {
            this.assetId = assetId;
            return this;
        }

        public ContractOffer build() {
            Objects.requireNonNull(id);

            // check mutual exclusivity
            if (policyId != null && policy != null) {
                throw new IllegalArgumentException("policy and policyId are mutually exclusive");
            }
            if (assetId != null && asset != null) {
                throw new IllegalArgumentException("asset and assetId are mutually exclusive");
            }

            if (policy == null && policyId == null) {
                throw new IllegalArgumentException("either policy or policyId must be set");
            }

            ContractOffer offer = new ContractOffer();
            offer.id = id;
            offer.policy = policy;
            offer.asset = asset;
            offer.provider = provider;
            offer.consumer = consumer;
            offer.offerStart = offerStart;
            offer.offerEnd = offerEnd;
            offer.contractStart = contractStart;
            offer.contractEnd = contractEnd;
            offer.assetId = assetId;
            offer.policyId = policyId;
            return offer;
        }
    }
}
