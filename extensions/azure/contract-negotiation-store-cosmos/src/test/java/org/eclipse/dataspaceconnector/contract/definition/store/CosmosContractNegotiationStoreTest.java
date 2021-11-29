package org.eclipse.dataspaceconnector.contract.definition.store;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;

import static org.easymock.EasyMock.strictMock;

class CosmosContractNegotiationStoreTest {
    private CosmosContractNegotiationStore store;
    private CosmosDbApi cosmosDbApiMock;

    @BeforeEach
    void setup() {
        cosmosDbApiMock = strictMock(CosmosDbApi.class);
        var typeManager = new TypeManager();
        var retryPolicy = new RetryPolicy<>();
        store = new CosmosContractNegotiationStore(cosmosDbApiMock, typeManager, retryPolicy, "test-connector");
    }

    //    @Test
    //    void findAll() {
    //        var doc1 = generateDocument();
    //        var doc2 = generateDocument();
    //        expect(cosmosDbApiMock.queryAllItems()).andReturn(List.of(doc1, doc2));
    //        replay(cosmosDbApiMock);
    //
    //        store.reload();
    //        var all = store.findAll();
    //        assertThat(all).hasSize(2).containsExactlyInAnyOrder(doc1.getWrappedInstance(), doc2.getWrappedInstance());
    //        verify(cosmosDbApiMock);
    //    }
    //
    //    @Test
    //    void findAll_noReload() {
    //        replay(cosmosDbApiMock);
    //
    //        var all = store.findAll();
    //        assertThat(all).isEmpty();
    //        verify(cosmosDbApiMock);
    //    }
    //
    //    @Test
    //    void save() {
    //        Capture<ContractNegotiationDocument> documentCapture = newCapture();
    //        cosmosDbApiMock.createItem(capture(documentCapture));
    //        expectLastCall().times(1);
    //        replay(cosmosDbApiMock);
    //
    //        var def = generateDefinition();
    //        store.save(def);
    //        assertThat(documentCapture.getValue().getWrappedInstance()).isEqualTo(def);
    //        verify(cosmosDbApiMock);
    //    }
    //
    //    @Test
    //    void save_verifyWriteThrough() {
    //        Capture<ContractNegotiationDocument> documentCapture = newCapture();
    //        cosmosDbApiMock.createItem(capture(documentCapture));
    //        expectLastCall().times(1);
    //        // cosmosDbApiQueryMock.queryAllItems() should never be called
    //        replay(cosmosDbApiMock);
    //
    //        var def = generateDefinition();
    //        store.save(def); //should write through the cache
    //        var all = store.findAll();
    //        assertThat(all).isNotEmpty().containsExactlyInAnyOrder(documentCapture.getValue().getWrappedInstance());
    //        verify(cosmosDbApiMock);
    //    }
    //
    //    @Test
    //    void update() {
    //        Capture<ContractNegotiationDocument> documentCapture = newCapture();
    //        cosmosDbApiMock.createItem(capture(documentCapture));
    //        expectLastCall().times(1);
    //        replay(cosmosDbApiMock);
    //
    //        var def = generateDefinition();
    //        store.update(def);
    //        assertThat(documentCapture.getValue().getWrappedInstance()).isEqualTo(def);
    //        verify(cosmosDbApiMock);
    //    }
    //
    //    @Test
    //    void delete() {
    //        cosmosDbApiMock.deleteItem(notNull());
    //        expectLastCall().times(1);
    //        replay(cosmosDbApiMock);
    //
    //        store.delete("some-id");
    //        verify(cosmosDbApiMock);
    //    }
    //
    //    @Test
    //    void reload() {
    //        replay(cosmosDbApiMock);
    //        verify(cosmosDbApiMock);
    //    }
}