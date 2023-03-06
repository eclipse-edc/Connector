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
 *       Fraunhofer Institute for Software and Systems Engineering - added tests
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractdefinition;

import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.SqlQuerySpec;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.model.ContractDefinitionDocument;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.TestFunctions.generateDefinition;
import static org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.TestFunctions.generateDocument;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.atLeast;
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
        var retryPolicy = RetryPolicy.ofDefaults();
        store = new CosmosContractDefinitionStore(cosmosDbApiMock, typeManager, retryPolicy, TEST_PART_KEY, mock(Monitor.class));
    }

    @Test
    void findAll() {
        var doc1 = generateDocument(TEST_PART_KEY);
        var doc2 = generateDocument(TEST_PART_KEY);
        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.of(doc1, doc2));

        store.reload();
        var all = store.findAll(QuerySpec.max());

        assertThat(all).hasSize(2).containsExactlyInAnyOrder(doc1.getWrappedInstance(), doc2.getWrappedInstance());
        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findAll_noReload() {
        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.empty());

        var all = store.findAll(QuerySpec.max());
        assertThat(all).isEmpty();
        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findById() {
        var doc = generateDocument(TEST_PART_KEY);
        when(cosmosDbApiMock.queryItemById(doc.getId())).thenReturn(doc);

        var result = store.findById(doc.getId());

        assertThat(result).isNotNull().isEqualTo(doc.getWrappedInstance());
        verify(cosmosDbApiMock).queryItemById(doc.getId());
    }

    @Test
    void findById_invalidId() {
        String invalidId = "invalid-id";
        when(cosmosDbApiMock.queryItemById(invalidId)).thenReturn(null);

        var result = store.findById(invalidId);

        assertThat(result).isNull();
        verify(cosmosDbApiMock).queryItemById(invalidId);
    }

    @Test
    void save() {
        var captor = ArgumentCaptor.forClass(CosmosDocument.class);
        doNothing().when(cosmosDbApiMock).createItem(captor.capture());
        var definition = generateDefinition();

        store.save(definition);

        assertThat(captor.getValue().getWrappedInstance()).isEqualTo(definition);
        verify(cosmosDbApiMock).createItem(captor.capture());
    }

    @Test
    void save_verifyWriteThrough() {
        var captor = ArgumentCaptor.forClass(CosmosDocument.class);
        doNothing().when(cosmosDbApiMock).createItem(captor.capture());
        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(IntStream.range(0, 1).mapToObj((i) -> captor.getValue()));
        // cosmosDbApiQueryMock.queryAllItems() should never be called
        var definition = generateDefinition();

        store.save(definition); //should write through the cache

        var all = store.findAll(QuerySpec.max());

        assertThat(all).isNotEmpty().containsExactlyInAnyOrder((ContractDefinition) captor.getValue().getWrappedInstance());
        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
        verify(cosmosDbApiMock).createItem(captor.capture());
    }

    @Test
    void update() {
        var doc = generateDocument(TEST_PART_KEY);
        var captor = ArgumentCaptor.forClass(CosmosDocument.class);
        doNothing().when(cosmosDbApiMock).updateItem(captor.capture());
        var definition = generateDefinition();
        when(cosmosDbApiMock.queryItemById(definition.getId())).thenReturn(doc);

        store.update(definition);

        assertThat(captor.getValue().getWrappedInstance()).isEqualTo(definition);
        verify(cosmosDbApiMock).updateItem(captor.capture());
    }

    @Test
    void deleteById_whenMissing_returnsNull() {

        when(cosmosDbApiMock.deleteItem("some-id")).thenThrow(new NotFoundException("not found"));

        var contractDefinition = store.deleteById("some-id");
        assertThat(contractDefinition.failed()).isTrue();
        assertThat(contractDefinition.reason()).isEqualTo(NOT_FOUND);
        verify(cosmosDbApiMock, atLeast(1)).deleteItem(notNull());
    }

    @Test
    void delete_whenContractDefinitionPresent_deletes() {
        var contractDefinition = generateDefinition();
        var document = new ContractDefinitionDocument(contractDefinition, TEST_PART_KEY);
        when(cosmosDbApiMock.deleteItem(document.getId())).thenReturn(document);

        var deletedDefinition = store.deleteById(document.getId());

        assertThat(deletedDefinition.succeeded()).isTrue();
        assertThat(deletedDefinition.getContent()).isEqualTo(contractDefinition);
    }

    @Test
    void delete_whenCosmoDbApiThrows_throws() {
        var id = "some-id";
        when(cosmosDbApiMock.deleteItem(id)).thenThrow(new EdcPersistenceException("Something went wrong"));
        assertThatThrownBy(() -> store.deleteById(id)).isInstanceOf(EdcPersistenceException.class);
    }

    @Test
    void findAll_noQuerySpec() {

        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PART_KEY)));

        var all = store.findAll(QuerySpec.Builder.newInstance().build());
        assertThat(all).hasSize(10);
        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifyFiltering() {
        var doc = generateDocument(TEST_PART_KEY);
        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.of(doc));

        var all = store.findAll(QuerySpec.Builder.newInstance().filter("id=" + doc.getId()).build());
        assertThat(all).hasSize(1).extracting(ContractDefinition::getId).containsOnly(doc.getId());
        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        assertThatThrownBy(() -> store.findAll(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting_asc() {
        var stream = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PART_KEY)).sorted(Comparator.comparing(ContractDefinitionDocument::getId).reversed()).map(Object.class::cast);
        when(cosmosDbApiMock.queryItems(any(SqlQuerySpec.class))).thenReturn(stream);

        var all = store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));

        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifySorting_desc() {
        var stream = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PART_KEY)).sorted(Comparator.comparing(ContractDefinitionDocument::getId)).map(Object.class::cast);
        when(cosmosDbApiMock.queryItems(isA(SqlQuerySpec.class))).thenReturn(stream);


        var all = store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build()).collect(Collectors.toList());
        assertThat(all).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractDefinition::getId));

        verify(cosmosDbApiMock).queryItems(any(SqlQuerySpec.class));
        verifyNoMoreInteractions(cosmosDbApiMock);
    }

    @Test
    void findAll_verifySorting_invalidField() {
        when(cosmosDbApiMock.queryItems(isA(SqlQuerySpec.class))).thenReturn(Stream.empty());

        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("nonexist").sortOrder(SortOrder.DESC).build())).isEmpty();
    }
}
