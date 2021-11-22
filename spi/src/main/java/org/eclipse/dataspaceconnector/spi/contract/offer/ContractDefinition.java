/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.spi.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Defines the parameters of a contract. Namely, the usage policy and asset selector that identifies the set of assets the contract applies to.
 *
 * Note that the id must be a UUID.
 */
public class ContractDefinition {
    private final String id;
    private final Policy usagePolicy;
    private final AssetSelectorExpression assetSelectorExpression;

    public ContractDefinition(@NotNull String id, @NotNull Policy usagePolicy, @NotNull AssetSelectorExpression expression) {
        this.id = Objects.requireNonNull(id);
        this.usagePolicy = Objects.requireNonNull(usagePolicy);
        this.assetSelectorExpression = Objects.requireNonNull(expression);
    }

    public String getId() {
        return id;
    }

    @NotNull
    public Policy getUsagePolicy() {
        return usagePolicy;
    }

    @NotNull
    public AssetSelectorExpression getAssetSelectorExpression() {
        return assetSelectorExpression;
    }
}
