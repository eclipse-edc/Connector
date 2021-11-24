package org.eclipse.dataspaceconnector.contract.definition.store;

import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosContractDefinitionStore implements ContractDefinitionStore {
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;

    public CosmosContractDefinitionStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        return with(retryPolicy).get((CheckedSupplier<List<Object>>) cosmosDbApi::queryAllItems)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Collection<ContractDefinition> definitions) {
        cosmosDbApi.createItems(definitions.stream().map(this::convertToDocument).collect(Collectors.toList()));
    }

    @Override
    public void save(ContractDefinition definition) {
        cosmosDbApi.createItem(convertToDocument(definition));
    }

    @Override
    public void update(ContractDefinition definition) {
        save(definition); //cosmos db api internally uses "upsert" semantics
    }

    @Override
    public void delete(String id) {
        cosmosDbApi.deleteItem(id);
    }

    @Override
    public void reload() {

    }

    @NotNull
    private ContractDefinitionDocument convertToDocument(ContractDefinition def) {
        return new ContractDefinitionDocument(def);
    }


    private ContractDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractDefinitionDocument.class).getWrappedInstance();
    }
}
