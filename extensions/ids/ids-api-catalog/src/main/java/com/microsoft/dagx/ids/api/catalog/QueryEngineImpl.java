package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.ids.spi.policy.IdsPolicyService;
import com.microsoft.dagx.spi.iam.ClaimToken;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;

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
