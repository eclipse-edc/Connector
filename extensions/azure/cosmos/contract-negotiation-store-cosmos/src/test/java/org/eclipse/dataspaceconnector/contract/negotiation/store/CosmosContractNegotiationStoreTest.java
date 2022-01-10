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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CosmosContractNegotiationStoreTest {
    private static final String PARTITION_KEY = "test-connector";
    private CosmosContractNegotiationStore store;
    private CosmosDbApi cosmosDbApi;

    @BeforeEach
    void setup() {
        cosmosDbApi = mock(CosmosDbApi.class);
        var typeManager = new TypeManager();
        var retryPolicy = new RetryPolicy<>();
        store = new CosmosContractNegotiationStore(cosmosDbApi, typeManager, retryPolicy, "test-connector");
    }

    @Test
    void find() {
        var doc = generateDocument();
        when(cosmosDbApi.queryItemById("test-id-1")).thenReturn(doc);

        var result = store.find("test-id-1");

        assertThat(result).usingRecursiveComparison().isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApi).queryItemById("test-id-1");
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void find_notFound() {
        when(cosmosDbApi.queryItemById(anyString())).thenReturn(null);

        assertThat(store.find("test-id-1")).isNull();
        verify(cosmosDbApi).queryItemById(anyString());
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void findForCorrelationId() {
        var doc = generateDocument();
        when(cosmosDbApi.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.of(doc));

        assertThat(store.findForCorrelationId("some-correlation-id")).usingRecursiveComparison().isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApi).queryItems(any(SqlQuerySpec.class));
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void findContractAgreement() {
        var doc = generateDocument();
        when(cosmosDbApi.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.of(doc));

        assertThat(store.findContractAgreement("test-contract-id")).usingRecursiveComparison().isEqualTo(doc.getWrappedInstance().getContractAgreement());
        verify(cosmosDbApi).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void save() {
        var negotiation = generateNegotiation();

        store.save(negotiation);

        verify(cosmosDbApi).saveItem(any(ContractNegotiationDocument.class));
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void delete() {
        store.delete("test-id");

        verify(cosmosDbApi).deleteItem("test-id");
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void nextForState() {
        var state = ContractNegotiationStates.CONFIRMED;
        when(cosmosDbApi.invokeStoredProcedure("nextForState", PARTITION_KEY, state.code(), 100, "test-connector"))
                .thenReturn("[]");

        var result = store.nextForState(state.code(), 100);

        assertThat(result).isEmpty();
        verify(cosmosDbApi).invokeStoredProcedure("nextForState", PARTITION_KEY, state.code(), 100, "test-connector");
        verifyNoMoreInteractions(cosmosDbApi);
    }
}