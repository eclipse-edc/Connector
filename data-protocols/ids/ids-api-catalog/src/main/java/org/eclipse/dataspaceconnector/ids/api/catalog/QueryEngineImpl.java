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
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;

import java.util.Collection;
import java.util.Collections;

import static java.util.stream.Collectors.toList;

/**
 *
 */
public class QueryEngineImpl implements QueryEngine {
    private final PolicyRegistry policyRegistry;
    private final IdsPolicyService policyService;
    private final MetadataStore metadataStore;
    private final Monitor monitor;

    public QueryEngineImpl(PolicyRegistry policyRegistry, IdsPolicyService policyService, MetadataStore metadataStore, Monitor monitor) {
        this.policyRegistry = policyRegistry;
        this.policyService = policyService;
        this.metadataStore = metadataStore;
        this.monitor = monitor;
    }

    @Override
    public Collection<DataEntry> execute(String correlationId, ClaimToken clientToken, String connectorId, String type, String query) {
        if (!"select *".equalsIgnoreCase(query)) {
            monitor.info("Invalid query: " + query);
            return Collections.emptyList();
        }
        // evaluate the policies the client satisfies
        var policies = policyRegistry.allPolicies().stream().filter(p -> policyService.evaluateRequest(connectorId, correlationId, clientToken, p).valid()).collect(toList());

        // execute the query and return the results
        return metadataStore.queryAll(policies);
    }
}
