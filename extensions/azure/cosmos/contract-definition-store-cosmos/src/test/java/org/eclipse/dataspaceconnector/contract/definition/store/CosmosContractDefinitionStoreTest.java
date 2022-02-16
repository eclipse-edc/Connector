package org.eclipse.dataspaceconnector.contract.definition.store;

import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.common.matchers.PredicateMatcher;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDefinition;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateDocument;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
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
        store.delete("some-id");

        verify(cosmosDbApiMock).deleteItem(notNull());
    }

    @Test
    void findAll_noQuerySpec() {

        when(cosmosDbApiMock.queryItems(isA(SqlQuerySpec.class))).thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument()));

        var all = store.findAll(QuerySpec.Builder.newInstance().build());
        assertThat(all).hasSize(10);
    }

    @Test
    void findAll_verifyPaging() {

        when(cosmosDbApiMock.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().equals("SELECT * FROM ContractDefinitionDocument OFFSET 3 LIMIT 4"))))).thenReturn(IntStream.range(0, 4).mapToObj(i -> generateDocument()));

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);


    }

    @Test
    void findAll_verifyPaging_tooLarge() {

        when(cosmosDbApiMock.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().equals("SELECT * FROM ContractDefinitionDocument OFFSET 5 LIMIT 100"))))).thenReturn(IntStream.range(0, 5).mapToObj(i -> generateDocument()));

        // page size too large
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);

        verify(cosmosDbApiMock).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().equals("SELECT * FROM ContractDefinitionDocument OFFSET 5 LIMIT 100"))));
    }

    @Test
    void findAll_verifyFiltering() {
        var doc = generateDocument();
        when(cosmosDbApiMock.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().startsWith("SELECT * FROM ContractDefinitionDocument WHERE ContractDefinitionDocument.wrappedInstance.id = @id")))))
                .thenReturn(Stream.of(doc));


        var all = store.findAll(QuerySpec.Builder.newInstance().filter("id=foobar").build());
        assertThat(all).hasSize(1).extracting(ContractDefinition::getId).containsOnly(doc.getId());
        verify(cosmosDbApiMock).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().startsWith("SELECT * FROM ContractDefinitionDocument WHERE ContractDefinitionDocument.wrappedInstance.id = @id"))));
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        assertThatThrownBy(() -> store.findAll(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting_asc() {
        when(cosmosDbApiMock.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractDefinitionDocument ORDER BY ContractDefinitionDocument.wrappedInstance.id DESC")))))
                .thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument()).sorted(Comparator.comparing(ContractDefinitionDocument::getId).reversed()).map(c -> c));

        var all = store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));

        verify(cosmosDbApiMock).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractDefinitionDocument ORDER BY ContractDefinitionDocument.wrappedInstance.id DESC"))));
    }

    @Test
    void findAll_verifySorting_desc() {
        when(cosmosDbApiMock.queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractDefinitionDocument ORDER BY ContractDefinitionDocument.wrappedInstance.id ASC")))))
                .thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument()).sorted(Comparator.comparing(ContractDefinitionDocument::getId)).map(c -> c));


        var all = store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractDefinition::getId));


        verify(cosmosDbApiMock).queryItems(argThat(new PredicateMatcher<SqlQuerySpec>(qs -> qs.getQueryText().contains("SELECT * FROM ContractDefinitionDocument ORDER BY ContractDefinitionDocument.wrappedInstance.id ASC"))));
    }

    @Test
    void findAll_verifySorting_invalidField() {
        when(cosmosDbApiMock.queryItems(isA(SqlQuerySpec.class))).thenReturn(Stream.empty());

        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("nonexist").sortOrder(SortOrder.DESC).build())).isEmpty();
    }
}