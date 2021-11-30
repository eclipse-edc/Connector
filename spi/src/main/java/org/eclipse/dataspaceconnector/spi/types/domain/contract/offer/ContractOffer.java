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
     */
    private Policy policy;

    /**
     * The offered asset
     */
    private Asset asset;

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

    @NotNull
    public Policy getPolicy() {
        return policy;
    }

    private ContractOffer() {
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
            this.offerStart = date;
            return this;
        }

        public Builder offerEnd(ZonedDateTime date) {
            this.offerEnd = date;
            return this;
        }

        public Builder contractStart(ZonedDateTime date) {
            this.contractStart = date;
            return this;
        }

        public Builder contractEnd(ZonedDateTime date) {
            this.contractEnd = date;
            return this;
        }

        public Builder policy(Policy policy) {
            this.policy = policy;
            return this;
        }

        public ContractOffer build() {
            Objects.requireNonNull(this.policy);
            Objects.requireNonNull(this.id);

            ContractOffer offer = new ContractOffer();
            offer.id = this.id;
            offer.policy = this.policy;
            offer.asset = this.asset;
            offer.provider = this.provider;
            offer.consumer = this.consumer;
            offer.offerStart = this.offerStart;
            offer.offerEnd = this.offerEnd;
            offer.contractStart = this.contractStart;
            offer.contractEnd = this.contractEnd;
            return offer;
        }
    }
}
