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

package org.eclipse.dataspaceconnector.core.defaults.contractdefinition;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;

class InMemoryContractDefinitionStoreTest {
    private final InMemoryContractDefinitionStore store = new InMemoryContractDefinitionStore();

    @Test
    void verifyStore() {
        var definition1 = ContractDefinition.Builder.newInstance().id("1").accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build();
        var definition2 = ContractDefinition.Builder.newInstance().id("2").accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build();

        store.save(definition1);
        assertThat(store.findAll(QuerySpec.max())).contains(definition1);

        store.save(List.of(definition2));
        assertThat(store.findAll(QuerySpec.max())).contains(definition1);

        var deletedDefinition = store.deleteById(definition1.getId());
        assertThat(deletedDefinition).isEqualTo(definition1);
        assertThat(store.findAll(QuerySpec.max())).doesNotContain(definition1);
    }

    @Test
    void deleteById_whenContractDefinitionMissing_returnsNull() {
        assertThat(store.deleteById("not-exists")).isNull();
    }

    @Test
    void findAll_defaultQuerySpec() {
        var all = IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).peek(store::save).collect(Collectors.toList());
        assertThat(store.findAll(QuerySpec.none())).containsExactlyInAnyOrder(all.toArray(new ContractDefinition[]{}));
    }

    @Test
    void findAll_verifyPaging() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(store::save);

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(4).limit(2).build())).hasSize(2);

        // page size larger than collection
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void findAll_verifyFiltering() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(store::save);
        assertThat(store.findAll(QuerySpec.Builder.newInstance().equalsAsContains(false).filter("id=id3").build())).extracting(ContractDefinition::getId)
                .containsOnly("id3");
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(store::save);
        assertThatThrownBy(() -> store.findAll(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(store::save);

        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractDefinition::getId));
        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void findAll_verifySorting_invalidProperty() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(store::save);
        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        // must actually collect, otherwise the stream is not materialized
        assertThat(store.findAll(query).collect(Collectors.toList())).isEmpty();
    }

    @Test
    void findById() {
        var id = "id";
        var definition = createContractDefinition(id);
        store.save(definition);

        var result = store.findById(id);

        assertThat(result).isNotNull().isEqualTo(definition);
    }

    @Test
    void findById_invalidId() {
        assertThat(store.findById("invalid-id")).isNull();
    }

    private ContractDefinition createContractDefinition(String id) {
        return ContractDefinition.Builder.newInstance().id(id).accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build();
    }
}
