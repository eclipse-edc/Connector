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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * {@link Contract} to regulate data transfer between two parties.
 */
public class Contract {

    private final String id;
    private final String providerId;
    private final String consumerId;
    private final long contractSigningDate;
    private final long contractStartDate;
    private final long contractEndDate;
    private final List<Asset> assets;
    private final Policy policy;

    private Contract(@NotNull String id,
                     @NotNull String providerId,
                     @NotNull String consumerId,
                     long contractSigningDate,
                     long contractStartDate,
                     long contractEndDate,
                     @NotNull List<Asset> assets,
                     @NotNull Policy policy) {
        this.id = Objects.requireNonNull(id);
        this.providerId = Objects.requireNonNull(providerId);
        this.consumerId = Objects.requireNonNull(consumerId);
        this.contractSigningDate = contractSigningDate;
        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;
        this.assets = Objects.requireNonNull(assets);
        this.policy = Objects.requireNonNull(policy);

        if (contractSigningDate == 0) {
            throw new IllegalArgumentException("contract signing date must be set");
        }
        if (contractStartDate == 0) {
            throw new IllegalArgumentException("contract start date must be set");
        }
        if (contractEndDate == 0) {
            throw new IllegalArgumentException("contract end date must be set");
        }
    }

    /**
     * Unique identifier of the {@link Contract}.
     *
     * @return contract id
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * The id of the data providing party.
     * Please note that id should be taken from the corresponding data ecosystem.
     * For example: In IDS the connector uses a URI from the IDS Information Model as ID. If the contract was
     * negotiated inside the IDS ecosystem, this URI should be used here.
     *
     * @return provider id
     */
    @NotNull
    public String getProviderId() {
        return providerId;
    }

    /**
     * The id of the data consuming party.
     * Please note that id should be taken from the corresponding contract ecosystem.
     * For example: In IDS the connector uses a URI from the IDS Information Model as ID. If the contract was
     * negotiated inside the IDS ecosystem, this URI should be used here.
     *
     * @return consumer id
     */
    @NotNull
    public String getConsumerId() {
        return consumerId;
    }

    /**
     * The date when the {@link Contract} has been signed. <br>
     * Numeric value representing the number of seconds from
     * 1970-01-01T00:00:00Z UTC until the specified UTC date/time.
     *
     * @return contract signing date
     */
    public long getContractSigningDate() {
        return contractSigningDate;
    }

    /**
     * The date from when the {@link Contract} is valid. <br>
     * Numeric value representing the number of seconds from
     * 1970-01-01T00:00:00Z UTC until the specified UTC date/time.
     *
     * @return contract start date
     */
    public long getContractStartDate() {
        return contractStartDate;
    }

    /**
     * The date until the {@link Contract} remains valid. <br>
     * Numeric value representing the number of seconds from
     * 1970-01-01T00:00:00Z UTC until the specified UTC date/time.
     *
     * @return contract end date
     */
    public long getContractEndDate() {
        return contractEndDate;
    }

    /**
     * All assets that are covered by the {@link Contract}.
     *
     * @return list of assets
     */
    @NotNull
    public List<Asset> getAssets() {
        return assets;
    }

    /**
     * A policy describing how the {@link Asset} of this contract may be used by the consumer.
     *
     * @return policy
     */
    @NotNull
    public Policy getPolicy() {
        return policy;
    }

    public static class Builder {

        private String id;
        private String providerId;
        private String consumerId;
        private long contractSigningDate;
        private long contractStartDate;
        private long contractEndDate;
        private List<Asset> assets;
        private Policy policy;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
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

        public Builder assets(List<Asset> assets) {
            this.assets = assets;
            return this;
        }

        public Builder policy(Policy policy) {
            this.policy = policy;
            return this;
        }

        public Contract build() {
            return new Contract(id, providerId, consumerId, contractSigningDate, contractStartDate, contractEndDate, assets, policy);
        }

    }
}
