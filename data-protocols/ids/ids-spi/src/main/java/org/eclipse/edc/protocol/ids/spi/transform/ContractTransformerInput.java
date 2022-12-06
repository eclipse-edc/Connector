/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.ids.spi.transform;

import de.fraunhofer.iais.eis.Contract;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Provides an input object for the IdsContractRequestToContractOfferTransformer.
 */
public class ContractTransformerInput {

    private final Contract contract;
    private final Asset asset;

    private ContractTransformerInput(
            @NotNull Contract contract,
            @NotNull Asset asset) {
        this.contract = Objects.requireNonNull(contract);
        this.asset = Objects.requireNonNull(asset);
    }

    public Contract getContract() {
        return contract;
    }

    public Asset getAsset() {
        return asset;
    }

    public static class Builder {
        private Contract contract;
        private Asset asset;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contract(Contract contract) {
            this.contract = contract;
            return this;
        }

        public Builder asset(Asset asset) {
            this.asset = asset;
            return this;
        }

        public ContractTransformerInput build() {
            return new ContractTransformerInput(contract, asset);
        }
    }
}
