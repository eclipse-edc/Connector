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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service;

import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.CONFLICT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractDefinitionServiceImplTest {

    private final ContractDefinitionStore store = mock(ContractDefinitionStore.class);
    private final ContractDefinitionLoader loader = mock(ContractDefinitionLoader.class);
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractDefinitionServiceImpl service = new ContractDefinitionServiceImpl(store, loader, transactionContext);

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
        assertThat(result).hasSize(1).first().matches(hasId(id));
    }

    @Test
    void create_shouldCreateDefinitionIfItDoesNotAlreadyExist() {
        var definition = createContractDefinition();
        when(store.findAll(isA(QuerySpec.class))).thenReturn(Stream.empty());

        var inserted = service.create(definition);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId(definition.getId()));
        verify(loader).accept(argThat(it -> definition.getId().equals(it.getId())));
    }

    @Test
    void create_shouldNotCreateDefinitionIfItAlreadyExists() {
        var definition = createContractDefinition();
        when(store.findById(definition.getId())).thenReturn(definition);

        var inserted = service.create(definition);

        assertThat(inserted.failed()).isTrue();
        assertThat(inserted.reason()).isEqualTo(CONFLICT);
        verifyNoInteractions(loader);
    }

    @Test
    void delete_shouldDeleteDefinitionIfItsNotReferencedByAnyAgreement() {
        var definition = createContractDefinition();
        when(store.deleteById("assetId")).thenReturn(definition);

        var deleted = service.delete("assetId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId(definition.getId()));
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