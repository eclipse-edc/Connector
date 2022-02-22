package org.eclipse.dataspaceconnector.contract.negotiation.store;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.core.type.TypeReference;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.dialect.SqlStatement;
import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

/**
 * Implementation of the {@link ContractDefinitionStore} based on CosmosDB. This store implements simple write-through
 * caching mechanics: read operations (e.g. findAll) hit the cache, while write operations affect both the cache AND the
 * database.
 */
public class CosmosContractNegotiationStore implements ContractNegotiationStore {
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final String connectorId;
    private final String partitionKey;

    public CosmosContractNegotiationStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy, String connectorId) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.connectorId = connectorId;
        partitionKey = connectorId;
    }

    @Override
    public @Nullable ContractNegotiation find(String negotiationId) {
        var object = with(retryPolicy).get(() -> cosmosDbApi.queryItemById(negotiationId));
        return object != null ? toNegotiation(object) : null;
    }


    @Override
    public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
        final String query = "SELECT * FROM c WHERE (c.wrappedInstance.correlationId = @corrId)";
        SqlParameter param = new SqlParameter("@corrId", correlationId);
        var querySpec = new SqlQuerySpec(query, param);

        //todo: throw exception if more than 1 element?
        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(querySpec));
        return objects.findFirst().map(this::toNegotiation).orElse(null);
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        final String query = "SELECT * FROM c WHERE c.wrappedInstance.contractAgreement.id = @contractId";
        SqlParameter param = new SqlParameter("@contractId", contractId);

        var spec = new SqlQuerySpec(query, param);
        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(spec));
        return objects.findFirst().map(o -> toNegotiation(o).getContractAgreement()).orElse(null);
    }

    @Override
    public void save(ContractNegotiation negotiation) {
        cosmosDbApi.saveItem(new ContractNegotiationDocument(negotiation, partitionKey));
    }

    @Override
    public void delete(String negotiationId) {
        cosmosDbApi.deleteItem(negotiationId);
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {

        String rawJson = cosmosDbApi.invokeStoredProcedure("nextForState", partitionKey, state, max, connectorId);
        var typeRef = new TypeReference<List<Object>>() {
        };
        var list = typeManager.readValue(rawJson, typeRef);
        return list.stream().map(this::toNegotiation).collect(Collectors.toList());
    }

    @Override
    public Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        var statement = new SqlStatement<>(ContractNegotiationDocument.class);
        var query = statement.where(querySpec.getFilterExpression())
                .offset(querySpec.getOffset())
                .limit(querySpec.getLimit())
                .orderBy(querySpec.getSortField(), querySpec.getSortOrder() == SortOrder.ASC)
                .getQueryAsSqlQuerySpec();

        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(query));
        return objects.map(this::toNegotiation);
    }


    private ContractNegotiation toNegotiation(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractNegotiationDocument.class).getWrappedInstance();
    }
}
