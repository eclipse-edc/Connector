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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A contract offer is exchanged between a providing and a consuming connector. It describes
 * the which assets the consumer may use, and the rules and policies that apply to each asset.
 */
public class ContractOffer {

    private List<OfferedAsset> assets;

    /**
     * The participant who provides the offered data
     */
    private URI provider;

    /**
     * The participant consuming the offered data
     */
    private URI consumer;

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

    @NotNull
    public List<OfferedAsset> getAssets() {
        return Optional.ofNullable(assets)
                .map(Collections::unmodifiableList)
                .orElseGet(Collections::emptyList);
    }

    public static final class Builder {
        private List<OfferedAsset> offeredAsset;
        private URI provider;
        private URI consumer;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder provider(final URI provider) {
            this.provider = provider;
            return this;
        }

        public Builder consumer(final URI consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder assets(final List<OfferedAsset> offeredAsset) {
            this.offeredAsset = offeredAsset;
            return this;
        }

        public ContractOffer build() {
            final ContractOffer offer = new ContractOffer();
            offer.assets = this.offeredAsset;
            offer.provider = this.provider;
            offer.consumer = this.consumer;
            return offer;
        }
    }
}