package org.eclipse.dataspaceconnector.contract.definition.store;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDefinition;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDocument;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CosmosContractDefinitionStoreTest {
    private CosmosContractDefinitionStore store;
    private CosmosDbApi cosmosDbApiMock;

    @BeforeEach
    void setup() {
        cosmosDbApiMock = mock(CosmosDbApi.class);
        var typeManager = new TypeManager();
        var retryPolicy = new RetryPolicy<>();
        store = new CosmosContractDefinitionStore(cosmosDbApiMock, typeManager, retryPolicy);
    }

    @Test
    void findAll() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();
        when(cosmosDbApiMock.queryAllItems()).thenReturn(List.of(doc1, doc2));

        store.reload();
        var all = store.findAll();

        assertThat(all).hasSize(2).containsExactlyInAnyOrder(doc1.getWrappedInstance(), doc2.getWrappedInstance());
        verify(cosmosDbApiMock).queryAllItems();
    }

    @Test
    void findAll_noReload() {
        when(cosmosDbApiMock.queryAllItems()).thenReturn(Collections.emptyList());

        var all = store.findAll();
        assertThat(all).isEmpty();
        verify(cosmosDbApiMock).queryAllItems();
    }

    @Test
    void save() {
        var captor = ArgumentCaptor.forClass(CosmosDocument.class);
        doNothing().when(cosmosDbApiMock).saveItem(captor.capture());
        when(cosmosDbApiMock.queryAllItems()).thenReturn(Collections.emptyList());
        var definition = generateDefinition();

        store.save(definition);

        assertThat(captor.getValue().getWrappedInstance()).isEqualTo(definition);
        verify(cosmosDbApiMock).queryAllItems();
        verify(cosmosDbApiMock).saveItem(captor.capture());
    }

    @Test
    void save_verifyWriteThrough() {
        var captor = ArgumentCaptor.forClass(CosmosDocument.class);
        doNothing().when(cosmosDbApiMock).saveItem(captor.capture());
        when(cosmosDbApiMock.queryAllItems()).thenReturn(Collections.emptyList());
        // cosmosDbApiQueryMock.queryAllItems() should never be called
        var definition = generateDefinition();

        store.save(definition); //should write through the cache

        var all = store.findAll();

        assertThat(all).isNotEmpty().containsExactlyInAnyOrder((ContractDefinition) captor.getValue().getWrappedInstance());
        verify(cosmosDbApiMock).queryAllItems();
        verify(cosmosDbApiMock).saveItem(captor.capture());
    }

    @Test
    void update() {
        var captor = ArgumentCaptor.forClass(CosmosDocument.class);
        doNothing().when(cosmosDbApiMock).saveItem(captor.capture());
        when(cosmosDbApiMock.queryAllItems()).thenReturn(Collections.emptyList());
        var definition = generateDefinition();

        store.update(definition);

        assertThat(captor.getValue().getWrappedInstance()).isEqualTo(definition);
        verify(cosmosDbApiMock).queryAllItems();
        verify(cosmosDbApiMock).saveItem(captor.capture());
    }

    @Test
    void delete() {
        var def = ContractDefinition.Builder.newInstance().id("some-id")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .contractPolicy(Policy.Builder.newInstance().build())
                .accessPolicy(Policy.Builder.newInstance().build())
                .build();

        store.delete(def);

        verify(cosmosDbApiMock).deleteItem(notNull());
    }

}