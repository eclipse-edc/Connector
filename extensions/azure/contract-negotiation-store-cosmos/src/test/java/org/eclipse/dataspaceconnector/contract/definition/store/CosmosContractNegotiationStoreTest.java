package org.eclipse.dataspaceconnector.contract.definition.store;

import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDocument;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateNegotiation;

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

    @Test
    void find() {
        var doc = generateDocument();
        expect(cosmosDbApiMock.queryItemById("test-id-1")).andReturn(doc);
        replay(cosmosDbApiMock);

        var result = store.find("test-id-1");
        assertThat(result).isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApiMock);

    }

    @Test
    void find_notFound() {
        expect(cosmosDbApiMock.queryItemById(anyString())).andReturn(null);
        replay(cosmosDbApiMock);
        assertThat(store.find("test-id-1")).isNull();
        verify(cosmosDbApiMock);

    }

    @Test
    void findForCorrelationId() {
        var doc = generateDocument();
        expect(cosmosDbApiMock.queryItems(anyObject(SqlQuerySpec.class))).andReturn(Stream.of(doc));
        replay(cosmosDbApiMock);

        assertThat(store.findForCorrelationId("some-correlation-id")).isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApiMock);

    }

    @Test
    void findContractAgreement() {
        var doc = generateDocument();
        expect(cosmosDbApiMock.queryItems(anyObject(SqlQuerySpec.class))).andReturn(Stream.of(doc));
        replay(cosmosDbApiMock);
        assertThat(store.findContractAgreement("test-contract-id")).isEqualTo(doc.getWrappedInstance().getContractAgreement());
        verify(cosmosDbApiMock);
    }

    @Test
    void save() {

        cosmosDbApiMock.saveItem(anyObject(ContractNegotiationDocument.class));
        expectLastCall();
        replay(cosmosDbApiMock);
        var n = generateNegotiation();
        store.save(n);
        verify(cosmosDbApiMock);

    }

    @Test
    void delete() {
        cosmosDbApiMock.deleteItem("test-id");
        expectLastCall();
        replay(cosmosDbApiMock);

        store.delete("test-id");
        verify(cosmosDbApiMock);
    }

    @Test
    void nextForState() {
        var state = ContractNegotiationStates.CONFIRMED;
        expect(cosmosDbApiMock.invokeStoredProcedure("nextForState", String.valueOf(state.code()), state.code(), 100, "test-connector"))
                .andReturn("[]");
        replay(cosmosDbApiMock);
        store.nextForState(state.code(), 100);
        verify(cosmosDbApiMock);
    }
}