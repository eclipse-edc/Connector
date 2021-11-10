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

package org.eclipse.dataspaceconnector.spi.types.domain.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A contract offer is exchanged between a providing and a consuming connector. It describes
 * the which assets the consumer may use, and the rules and policies that apply to each asset.
 */
@JsonDeserialize(builder = ContractOffer.Builder.class)
public class ContractOffer {

    /**
     * The policy that describes the usage conditions of the assets
     */
    private Policy policy;

    /**
     * The offered assets@
     */
    private List<Asset> assets;

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

    private ContractOffer() {
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
    public List<Asset> getAssets() {
        return assets == null ? Collections.emptyList() : Collections.unmodifiableList(assets);
    }

    @NotNull
    public Policy getPolicy() {
        return policy;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private List<Asset> assets;
        private Policy policy;
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

        public Builder provider(URI provider) {
            this.provider = provider;
            return this;
        }

        public Builder consumer(URI consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder assets(List<Asset> assets) {
            this.assets = assets;
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

            ContractOffer offer = new ContractOffer();
            offer.policy = this.policy;
            offer.assets = this.assets;
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