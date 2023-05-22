/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation;

import com.azure.cosmos.models.SqlQuerySpec;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.model.ContractNegotiationDocument;
import org.eclipse.edc.junit.matchers.PredicateMatcher;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.TestFunctions.createNegotiationBuilder;
import static org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.TestFunctions.generateDocument;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class CosmosContractNegotiationStoreTest {
    private static final String PARTITION_KEY = "test-connector";
    private final Clock clock = Clock.systemUTC();
    private CosmosContractNegotiationStore store;
    private CosmosDbApi cosmosDbApi;

    @BeforeEach
    void setup() {
        cosmosDbApi = mock(CosmosDbApi.class);
        var typeManager = new TypeManager();
        var retryPolicy = RetryPolicy.ofDefaults();
        store = new CosmosContractNegotiationStore(cosmosDbApi, typeManager, retryPolicy, "test-connector", clock);
    }

    @Test
    void find() {
        var doc = generateDocument();
        when(cosmosDbApi.queryItemById("test-id-1")).thenReturn(doc);

        var result = store.findById("test-id-1");

        assertThat(result).usingRecursiveComparison().isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApi).queryItemById("test-id-1");
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void find_notFound() {
        when(cosmosDbApi.queryItemById(anyString())).thenReturn(null);

        assertThat(store.findById("test-id-1")).isNull();
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
        var negotiation = TestFunctions.createNegotiation();

        store.save(negotiation);

        verify(cosmosDbApi).queryItemById(eq(negotiation.getId()));
        verify(cosmosDbApi).createItem(any(ContractNegotiationDocument.class));
        verify(cosmosDbApi, times(2)).invokeStoredProcedure(eq("lease"), eq(PARTITION_KEY), any(), any(), any());
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void delete() {
        var cn = createNegotiationBuilder("test-id").build();
        when(cosmosDbApi.queryItemById(any())).thenReturn(generateDocument(cn));

        store.delete("test-id");

        verify(cosmosDbApi).queryItemById("test-id");
        verify(cosmosDbApi).deleteItem("test-id");
        verify(cosmosDbApi, times(2)).invokeStoredProcedure(eq("lease"), eq(PARTITION_KEY), any(), any(), any());
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void delete_hasAgreement() {
        when(cosmosDbApi.queryItemById(any())).thenReturn(generateDocument());

        assertThatThrownBy(() -> store.delete("test-id")).isInstanceOf(IllegalStateException.class);

        verify(cosmosDbApi).queryItemById("test-id");
        verify(cosmosDbApi, never()).deleteItem("test-id");
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void delete_notFound() {
        when(cosmosDbApi.queryItemById(any())).thenReturn(null);

        store.delete("test-id");

        verify(cosmosDbApi).queryItemById("test-id");
        verify(cosmosDbApi, never()).deleteItem("test-id");
        verifyNoMoreInteractions(cosmosDbApi);
    }

    @Test
    void nextNotLeased() {
        var state = ContractNegotiationStates.AGREED;
        when(cosmosDbApi.invokeStoredProcedure("nextForState", PARTITION_KEY, state.code(), 100, "test-connector"))
                .thenReturn("[]");

        var result = store.nextNotLeased(100, hasState(state.code()));

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
