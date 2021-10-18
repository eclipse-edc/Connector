/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ids.api.catalog;

import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;
import java.util.Collections;

import static java.util.stream.Collectors.toList;

public class QueryEngineImpl implements QueryEngine {
    private final PolicyRegistry policyRegistry;
    private final IdsPolicyService policyService;
    private final AssetIndex assetIndex;
    private final Monitor monitor;

    public QueryEngineImpl(PolicyRegistry policyRegistry, IdsPolicyService policyService, AssetIndex assetIndex, Monitor monitor) {
        this.policyRegistry = policyRegistry;
        this.policyService = policyService;
        this.assetIndex = assetIndex;
        this.monitor = monitor;
    }

    @Override
    public Collection<Asset> execute(String correlationId, ClaimToken consumerToken, String connectorId, String type, String query) {
        if (!"select *".equalsIgnoreCase(query)) {
            monitor.info("Invalid query: " + query);
            return Collections.emptyList();
        }

        // TODO: mapping policies to asset selector expression
        var policies = policyRegistry.allPolicies().stream()
                .filter(p -> policyService.evaluateRequest(connectorId, correlationId, consumerToken, p).valid())
                .collect(toList());

        return assetIndex.queryAssets(AssetSelectorExpression.Builder.newInstance().build()).collect(toList());
    }
}
