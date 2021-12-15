package org.eclipse.dataspaceconnector.contract.negotiation.store;

import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.contract.negotiation.store.TestFunctions.generateDocument;
import static org.eclipse.dataspaceconnector.contract.negotiation.store.TestFunctions.generateNegotiation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CosmosContractNegotiationStoreTest {
    private CosmosContractNegotiationStore store;
    private CosmosDbApi cosmosDbApiMock;

    @BeforeEach
    void setup() {
        cosmosDbApiMock = mock(CosmosDbApi.class);
        var typeManager = new TypeManager();
        var retryPolicy = new RetryPolicy<>();
        store = new CosmosContractNegotiationStore(cosmosDbApiMock, typeManager, retryPolicy, "test-connector");
    }

    @Test
    void find() {
        var doc = generateDocument();
        when(cosmosDbApiMock.queryItemById("test-id-1")).thenReturn(doc);

        var result = store.find("test-id-1");

        assertThat(result).usingRecursiveComparison().isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApiMock).queryItemById("test-id-1");
    }

    @Test
    void find_notFound() {
        when(cosmosDbApiMock.queryItemById(anyString())).thenReturn(null);

        assertThat(store.find("test-id-1")).isNull();
        verify(cosmosDbApiMock).queryItemById(anyString());
    }

    @Test
    void findForCorrelationId() {
        var doc = generateDocument();
        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.of(doc));

        assertThat(store.findForCorrelationId("some-correlation-id")).usingRecursiveComparison().isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findContractAgreement() {
        var doc = generateDocument();
        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.of(doc));

        assertThat(store.findContractAgreement("test-contract-id")).usingRecursiveComparison().isEqualTo(doc.getWrappedInstance().getContractAgreement());
        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void save() {
        var negotiation = generateNegotiation();

        store.save(negotiation);

        verify(cosmosDbApiMock).saveItem(any(ContractNegotiationDocument.class));
    }

    @Test
    void delete() {
        store.delete("test-id");

        verify(cosmosDbApiMock).deleteItem("test-id");
    }

    @Test
    void nextForState() {
        var state = ContractNegotiationStates.CONFIRMED;
        when(cosmosDbApiMock.invokeStoredProcedure("nextForState", String.valueOf(state.code()), state.code(), 100, "test-connector"))
                .thenReturn("[]");

        var result = store.nextForState(state.code(), 100);

        assertThat(result).isEmpty();
        verify(cosmosDbApiMock).invokeStoredProcedure("nextForState", String.valueOf(state.code()), state.code(), 100, "test-connector");
    }
}