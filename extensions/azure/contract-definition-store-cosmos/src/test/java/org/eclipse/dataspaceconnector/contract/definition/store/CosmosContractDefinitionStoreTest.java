package org.eclipse.dataspaceconnector.contract.definition.store;

import net.jodah.failsafe.RetryPolicy;
import org.easymock.Capture;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDefinition;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDocument;

class CosmosContractDefinitionStoreTest {
    private CosmosContractDefinitionStore store;
    private CosmosDbApi cosmosDbApiMock;

    @BeforeEach
    void setup() {
        cosmosDbApiMock = strictMock(CosmosDbApi.class);
        var typeManager = new TypeManager();
        var retryPolicy = new RetryPolicy<>();
        store = new CosmosContractDefinitionStore(cosmosDbApiMock, typeManager, retryPolicy);
    }

    @Test
    void findAll() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();
        expect(cosmosDbApiMock.queryAllItems()).andReturn(List.of(doc1, doc2));
        replay(cosmosDbApiMock);

        var all = store.findAll();
        assertThat(all).hasSize(2).containsExactlyInAnyOrder(doc1.getWrappedInstance(), doc2.getWrappedInstance());
        verify(cosmosDbApiMock);
    }

    @Test
    void save() {
        Capture<ContractDefinitionDocument> documentCapture = newCapture();
        cosmosDbApiMock.createItem(capture(documentCapture));
        expectLastCall().times(1);
        replay(cosmosDbApiMock);

        var def = generateDefinition();
        store.save(def);
        assertThat(documentCapture.getValue().getWrappedInstance()).isEqualTo(def);
        verify(cosmosDbApiMock);
    }

    @Test
    void update() {
        Capture<ContractDefinitionDocument> documentCapture = newCapture();
        cosmosDbApiMock.createItem(capture(documentCapture));
        expectLastCall().times(1);
        replay(cosmosDbApiMock);

        var def = generateDefinition();
        store.update(def);
        assertThat(documentCapture.getValue().getWrappedInstance()).isEqualTo(def);
        verify(cosmosDbApiMock);
    }

    @Test
    void delete() {
        cosmosDbApiMock.deleteItem(notNull());
        expectLastCall().times(1);
        replay(cosmosDbApiMock);

        store.delete("some-id");
        verify(cosmosDbApiMock);
    }

    @Test
    void reload() {
        replay(cosmosDbApiMock);
        verify(cosmosDbApiMock);
    }
}