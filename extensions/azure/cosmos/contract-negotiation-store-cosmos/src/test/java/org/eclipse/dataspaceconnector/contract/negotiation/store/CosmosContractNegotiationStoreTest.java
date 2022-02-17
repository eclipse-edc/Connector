package org.eclipse.dataspaceconnector.contract.negotiation.store;

import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.common.matchers.PredicateMatcher;
import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.contract.negotiation.store.TestFunctions.generateDocument;
import static org.eclipse.dataspaceconnector.contract.negotiation.store.TestFunctions.generateNegotiation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
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

    @Test
    void findAll_noQuerySpec() {

        when(cosmosDbApi.queryItems(isA(SqlQuerySpec.class))).thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument()));

        var all = store.queryNegotiations(QuerySpec.Builder.newInstance().build());
        assertThat(all).hasSize(10);
    }

    @Test
    void findAll_verifyPaging() {

        when(cosmosDbApi.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().equals("SELECT * FROM ContractNegotiationDocument OFFSET 3 LIMIT 4"))))).thenReturn(IntStream.range(0, 4).mapToObj(i -> generateDocument()));

        // page size fits
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);


    }

    @Test
    void findAll_verifyPaging_tooLarge() {

        when(cosmosDbApi.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().equals("SELECT * FROM ContractNegotiationDocument OFFSET 5 LIMIT 100"))))).thenReturn(IntStream.range(0, 5).mapToObj(i -> generateDocument()));

        // page size too large
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);

        verify(cosmosDbApi).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().equals("SELECT * FROM ContractNegotiationDocument OFFSET 5 LIMIT 100"))));
    }

    @Test
    void findAll_verifyFiltering() {
        var doc = generateDocument();
        when(cosmosDbApi.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().startsWith("SELECT * FROM ContractNegotiationDocument WHERE ContractNegotiationDocument.wrappedInstance.id = @id")))))
                .thenReturn(Stream.of(doc));


        var all = store.queryNegotiations(QuerySpec.Builder.newInstance().filter("id=foobar").build());
        assertThat(all).hasSize(1).extracting(ContractNegotiation::getId).containsOnly(doc.getId());
        verify(cosmosDbApi).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().startsWith("SELECT * FROM ContractNegotiationDocument WHERE ContractNegotiationDocument.wrappedInstance.id = @id"))));
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        assertThatThrownBy(() -> store.queryNegotiations(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting_asc() {
        when(cosmosDbApi.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractNegotiationDocument ORDER BY ContractNegotiationDocument.wrappedInstance.id DESC")))))
                .thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument()).sorted(Comparator.comparing(ContractNegotiationDocument::getId).reversed()).map(c -> c));

        var all = store.queryNegotiations(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));

        verify(cosmosDbApi).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractNegotiationDocument ORDER BY ContractNegotiationDocument.wrappedInstance.id DESC"))));
    }

    @Test
    void findAll_verifySorting_desc() {
        when(cosmosDbApi.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractNegotiationDocument ORDER BY ContractNegotiationDocument.wrappedInstance.id ASC")))))
                .thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument()).sorted(Comparator.comparing(ContractNegotiationDocument::getId)).map(c -> c));


        var all = store.queryNegotiations(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractNegotiation::getId));


        verify(cosmosDbApi).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractNegotiationDocument ORDER BY ContractNegotiationDocument.wrappedInstance.id ASC"))));
    }

    @Test
    void findAll_verifySorting_invalidField() {
        when(cosmosDbApi.queryItems(isA(SqlQuerySpec.class))).thenReturn(Stream.empty());

        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().sortField("nonexist").sortOrder(SortOrder.DESC).build())).isEmpty();
    }
}