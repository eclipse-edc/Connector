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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A contract offer is exchanged between two participant agents. It describes the which assets the consumer may use, and the rules and policies that apply to each asset.
 */
@JsonDeserialize(builder = ContractOffer.Builder.class)
public class ContractOffer {

    public static final String PROPERTY_MESSAGE_ID = "contract-offer:prop:message-id";

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
    /**
     * Properties of the contract offer. Properties are not send to the other connector and are intended to help the connector manage contract offers.
     */
    private Map<String, String> properties;

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

    @NotNull
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Nullable
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, policy, asset, properties, provider, consumer, offerStart, offerEnd, contractStart, contractEnd);
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
                Objects.equals(properties, that.properties) && Objects.equals(contractStart, that.contractStart) && Objects.equals(contractEnd, that.contractEnd);
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
        private final Map<String, String> properties;

        private Builder() {
            properties = new HashMap<>();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public static Builder copy(ContractOffer contractOffer) {
            return new Builder().id(contractOffer.id).properties(contractOffer.properties).asset(contractOffer.asset).policy(contractOffer.policy).provider(contractOffer.provider).consumer(contractOffer.consumer).offerStart(contractOffer.offerStart)
                    .offerEnd(contractOffer.offerEnd).contractStart(contractOffer.contractStart).contractEnd(contractOffer.contractEnd);
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

        public Builder properties(Map<String, String> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public Builder property(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public ContractOffer build() {
            Objects.requireNonNull(policy);
            Objects.requireNonNull(id);

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
            offer.properties = this.properties;
            return offer;
        }
    }
}
