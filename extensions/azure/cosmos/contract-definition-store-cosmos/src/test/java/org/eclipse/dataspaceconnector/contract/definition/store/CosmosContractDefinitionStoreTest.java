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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.contract.definition.store;

import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDocument;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CosmosContractDefinitionStoreTest {
    private static final String TEST_PART_KEY = "test_part_key";
    private CosmosContractDefinitionStore store;
    private CosmosDbApi cosmosDbApiMock;

    @BeforeEach
    void setup() {
        cosmosDbApiMock = mock(CosmosDbApi.class);
        var typeManager = new TypeManager();
        var retryPolicy = new RetryPolicy<>();
        store = new CosmosContractDefinitionStore(cosmosDbApiMock, typeManager, retryPolicy, TEST_PART_KEY);
    }

    @Test
    void findAll() {
        var doc1 = generateDocument(TEST_PART_KEY);
        var doc2 = generateDocument(TEST_PART_KEY);
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

        when(cosmosDbApiMock.queryAllItems()).thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PART_KEY)).collect(Collectors.toList()));

        var all = store.findAll(QuerySpec.Builder.newInstance().build());
        assertThat(all).hasSize(10);
        verify(cosmosDbApiMock).queryAllItems();
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifyPaging() {
        when(cosmosDbApiMock.queryAllItems()).thenReturn(IntStream.range(0, 4).mapToObj(i -> generateDocument(TEST_PART_KEY)).collect(Collectors.toList()));

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(1);
        verify(cosmosDbApiMock).queryAllItems();
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifyPaging_tooLarge() {
        when(cosmosDbApiMock.queryAllItems()).thenReturn(IntStream.range(0, 15).mapToObj(i -> generateDocument(TEST_PART_KEY)).collect(Collectors.toList()));

        // page size too large
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(10);

        verify(cosmosDbApiMock).queryAllItems();
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifyFiltering() {
        var doc = generateDocument(TEST_PART_KEY);
        when(cosmosDbApiMock.queryAllItems()).thenReturn(List.of(doc));

        var all = store.findAll(QuerySpec.Builder.newInstance().filter("id=" + doc.getId()).build());
        assertThat(all).hasSize(1).extracting(ContractDefinition::getId).containsOnly(doc.getId());
        verify(cosmosDbApiMock).queryAllItems();
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        assertThatThrownBy(() -> store.findAll(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting_asc() {
        when(cosmosDbApiMock.queryAllItems()).thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PART_KEY)).sorted(Comparator.comparing(ContractDefinitionDocument::getId).reversed()).collect(Collectors.toList()));

        var all = store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));

        verify(cosmosDbApiMock).queryAllItems();
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifySorting_desc() {
        when(cosmosDbApiMock.queryAllItems()).thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PART_KEY)).sorted(Comparator.comparing(ContractDefinitionDocument::getId)).collect(Collectors.toList()));


        var all = store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractDefinition::getId));

        verify(cosmosDbApiMock).queryAllItems();
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifySorting_invalidField() {
        when(cosmosDbApiMock.queryItems(isA(SqlQuerySpec.class))).thenReturn(Stream.empty());

        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("nonexist").sortOrder(SortOrder.DESC).build())).isEmpty();
    }
}