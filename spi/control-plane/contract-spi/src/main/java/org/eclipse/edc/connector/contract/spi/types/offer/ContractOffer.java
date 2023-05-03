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

package org.eclipse.edc.connector.contract.spi.types.offer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A contract offer is exchanged between two participant agents. It describes the which assets the consumer may use, and
 * the rules and policies that apply to each asset.
 */
@JsonDeserialize(builder = ContractOffer.Builder.class)
public class ContractOffer {
    private String id;

    /**
     * The policy that describes the usage conditions of the assets
     */
    private Policy policy;

    /**
     * The offered asset
     */
    private Asset asset;
    /**
     * The participant who provides the offered data
     */
    private String providerId;
    /**
     * The participant consuming the offered data
     */
    private String consumerId;
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
     *
     * @deprecated replaced with policy implementation
     */
    @Deprecated(forRemoval = true)
    private ZonedDateTime contractStart;
    /**
     * Timestamp defining the end date when the contract becomes terminated
     *
     * @deprecated replaced with policy implementation
     */
    @Deprecated(forRemoval = true)
    private ZonedDateTime contractEnd;


    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getProviderId() {
        return providerId;
    }

    @Nullable
    public String getConsumerId() {
        return consumerId;
    }

    @Nullable
    public ZonedDateTime getOfferStart() {
        return offerStart;
    }

    @Nullable
    public ZonedDateTime getOfferEnd() {
        return offerEnd;
    }

    @Deprecated
    @NotNull
    public ZonedDateTime getContractStart() {
        return contractStart;
    }

    @Deprecated
    @NotNull
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
    public int hashCode() {
        return Objects.hash(id, policy, asset, providerId, consumerId, offerStart, offerEnd, contractStart, contractEnd);
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
        return Objects.equals(id, that.id) && Objects.equals(policy, that.policy) && Objects.equals(asset, that.asset) && Objects.equals(providerId, that.providerId) &&
                Objects.equals(consumerId, that.consumerId) && Objects.equals(offerStart, that.offerStart) && Objects.equals(offerEnd, that.offerEnd) &&
                Objects.equals(contractStart, that.contractStart) && Objects.equals(contractEnd, that.contractEnd);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final ContractOffer contractOffer;

        private Builder() {
            contractOffer = new ContractOffer();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            contractOffer.id = id;
            return this;
        }

        public Builder providerId(String providerId) {
            contractOffer.providerId = providerId;
            return this;
        }

        public Builder consumerId(String consumerId) {
            contractOffer.consumerId = consumerId;
            return this;
        }

        public Builder asset(Asset asset) {
            contractOffer.asset = asset;
            return this;
        }

        public Builder offerStart(ZonedDateTime date) {
            contractOffer.offerStart = date;
            return this;
        }

        public Builder offerEnd(ZonedDateTime date) {
            contractOffer.offerEnd = date;
            return this;
        }

        @Deprecated
        public Builder contractStart(ZonedDateTime date) {
            contractOffer.contractStart = date;
            return this;
        }

        @Deprecated
        public Builder contractEnd(ZonedDateTime date) {
            contractOffer.contractEnd = date;
            return this;
        }

        public Builder policy(Policy policy) {
            contractOffer.policy = policy;
            return this;
        }

        public ContractOffer build() {
            Objects.requireNonNull(contractOffer.id);
            Objects.requireNonNull(contractOffer.asset, "Asset must not be null");
            Objects.requireNonNull(contractOffer.policy, "Policy must not be null");
            return contractOffer;
        }
    }
}
