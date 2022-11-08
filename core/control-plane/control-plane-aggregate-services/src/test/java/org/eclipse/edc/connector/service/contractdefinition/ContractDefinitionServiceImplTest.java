/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.service.contractdefinition;

import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionListener;
import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionObservable;
import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionObservableImpl;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractDefinitionServiceImplTest {

    private final ContractDefinitionStore store = mock(ContractDefinitionStore.class);
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractDefinitionObservable observable = new ContractDefinitionObservableImpl();
    private final ContractDefinitionListener listener = mock(ContractDefinitionListener.class);

    private final ContractDefinitionServiceImpl service = new ContractDefinitionServiceImpl(store, transactionContext, observable);

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
    }

    @Test
    void findById_filtersById() {
        var definition = createContractDefinition();
        when(store.findById(definition.getId())).thenReturn(definition);

        var result = service.findById(definition.getId());

        assertThat(result).matches(hasId(definition.getId()));
    }

    @Test
    void findById_returnsNullIfNotFound() {
        when(store.findById(anyString())).thenReturn(null);

        var result = service.findById("any");

        assertThat(result).isNull();
    }

    @Test
    void query() {
        var definition = createContractDefinition();
        when(store.findAll(isA(QuerySpec.class))).thenReturn(Stream.of(definition));

        var result = service.query(QuerySpec.none());

        String id = definition.getId();
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(1).first().matches(hasId(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "selectorExpression.criteria.leftHand=foo", //invalid path
            "accessPolicyId'LIKE/**/?/**/LIMIT/**/?/**/OFFSET/**/?;DROP/**/TABLE/**/test/**/--%20=%20ABC--", //some SQL injection
    })
    void query_invalidFilter(String invalidFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(invalidFilter)
                .build();
        assertThat(service.query(query).failed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "selectorExpression.criteria.operandLeft=foo", //invalid path
            "selectorExpression.criteria.operator=LIKE", //invalid path
            "selectorExpression.criteria.operandRight=bar" //invalid path
    })
    void query_validFilter(String validFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(validFilter)
                .build();
        service.query(query);
        verify(store).findAll(query);
    }

    @Test
    void create_shouldCreateDefinitionIfItDoesNotAlreadyExist() {
        var definition = createContractDefinition();
        when(store.findAll(isA(QuerySpec.class))).thenReturn(Stream.empty());

        var inserted = service.create(definition);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId(definition.getId()));
        verify(store).accept(argThat(it -> definition.getId().equals(it.getId())));
        verify(listener).created(any());
    }

    @Test
    void create_shouldNotCreateDefinitionIfItAlreadyExists() {
        var definition = createContractDefinition();
        when(store.findById(definition.getId())).thenReturn(definition);

        var inserted = service.create(definition);

        assertThat(inserted.failed()).isTrue();
        assertThat(inserted.reason()).isEqualTo(CONFLICT);
        verifyNoInteractions(listener);
    }

    @Test
    void delete_shouldDeleteDefinitionIfItsNotReferencedByAnyAgreement() {
        var definition = createContractDefinition();
        when(store.deleteById("assetId")).thenReturn(definition);

        var deleted = service.delete("assetId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId(definition.getId()));
        verify(listener).deleted(any());
    }

    @NotNull
    private Predicate<ContractDefinition> hasId(String id) {
        return d -> d.getId().equals(id);
    }

    private ContractDefinition createContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();
    }
}
