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

package org.eclipse.edc.connector.controlplane.services.contractdefinition;

import org.eclipse.edc.connector.controlplane.contract.spi.definition.observe.ContractDefinitionListener;
import org.eclipse.edc.connector.controlplane.contract.spi.definition.observe.ContractDefinitionObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.definition.observe.ContractDefinitionObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractDefinitionServiceImplTest {

    private final ContractDefinitionStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractDefinitionObservable observable = new ContractDefinitionObservableImpl();
    private final ContractDefinitionListener listener = mock();
    private final QueryValidator queryValidator = mock();

    private final ContractDefinitionService service = new ContractDefinitionServiceImpl(store, transactionContext, observable, queryValidator);

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
    void search() {
        var definition = createContractDefinition();
        when(store.findAll(isA(QuerySpec.class))).thenReturn(Stream.of(definition));
        when(queryValidator.validate(any())).thenReturn(Result.success());

        var result = service.search(QuerySpec.none());

        assertThat(result).isSucceeded().asInstanceOf(list(ContractDefinition.class))
                .hasSize(1).first().matches(hasId(definition.getId()));
    }

    @Test
    void search_shouldFail_whenQueryIsNotValid() {
        when(queryValidator.validate(any())).thenReturn(Result.failure("not valid"));

        var result = service.search(QuerySpec.none());

        assertThat(result).isFailed();
        verifyNoInteractions(store);
    }

    @Test
    void create_shouldCreateDefinitionIfItDoesNotAlreadyExist() {
        var definition = createContractDefinition();
        when(store.findAll(isA(QuerySpec.class))).thenReturn(Stream.empty());
        when(store.save(definition)).thenReturn(StoreResult.success());

        var inserted = service.create(definition);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId(definition.getId()));
        verify(store).save(argThat(it -> definition.getId().equals(it.getId())));
        verify(listener).created(any());
    }

    @Test
    void create_shouldNotCreateDefinitionIfItAlreadyExists() {
        var definition = createContractDefinition();
        when(store.save(definition)).thenReturn(StoreResult.alreadyExists(""));

        var inserted = service.create(definition);

        assertThat(inserted.failed()).isTrue();
        assertThat(inserted.reason()).isEqualTo(CONFLICT);
        verifyNoInteractions(listener);
    }

    @Test
    void create_shouldNotCreateDefinitionIfTheStoreFails() {
        var definition = createContractDefinition();
        when(store.findById(definition.getId())).thenReturn(null);
        when(store.save(definition)).thenReturn(StoreResult.alreadyExists("Exists"));

        var inserted = service.create(definition);

        assertThat(inserted.failed()).isTrue();
        assertThat(inserted.reason()).isEqualTo(CONFLICT);
        verifyNoInteractions(listener);
    }

    @Test
    void delete_shouldDeleteDefinitionIfItsNotReferencedByAnyAgreement() {
        var definition = createContractDefinition();
        when(store.deleteById("assetId")).thenReturn(StoreResult.success(definition));

        var deleted = service.delete("assetId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId(definition.getId()));
        verify(listener).deleted(any());
    }


    @Test
    void update_shouldUpdate_whenExists() {
        var definition = createContractDefinition();
        when(store.update(definition)).thenReturn(StoreResult.success());

        var updated = service.update(definition);

        assertThat(updated.succeeded()).isTrue();
        verify(store).update(eq(definition));
        verify(listener).updated(any());
    }

    @Test
    void update_shouldReturnNotFound_whenNotExists() {
        var definition = createContractDefinition();
        when(store.update(definition)).thenReturn(StoreResult.notFound(""));

        var updated = service.update(definition);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);
        verify(listener, never()).updated(any());
    }

    @Test
    void update_shouldReturnNotFound_whenTheStoreFails() {
        var definition = createContractDefinition();
        when(store.update(definition)).thenReturn(StoreResult.notFound("Not found"));

        var updated = service.update(definition);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);
        verify(listener, never()).updated(any());
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
                .build();
    }
}
