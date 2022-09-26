/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Tests
 *       Microsoft Corporation - Method signature change
 *       Microsoft Corporation - refactoring
 *       Microsoft Corporation - added tests
 *       Fraunhofer Institute for Software and Systems Engineering - added tests
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.contract.offer.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.contract.offer.store.TestFunctions.createContractDefinition;
import static org.eclipse.dataspaceconnector.contract.offer.store.TestFunctions.createContractDefinitions;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;

public abstract class ContractDefinitionStoreTestBase {

    public ContractDefinitionStoreTestBase() {
        System.setProperty("contractdefinitionstore.supports.collectionQuery", String.valueOf(supportsCollectionQuery()));
        System.setProperty("contractdefinitionstore.supports.sortorder", String.valueOf(supportsSortOrder()));
        System.setProperty("contractdefinitionstore.supports.collectionIndexQuery", String.valueOf(supportsCollectionIndexQuery()));

    }

    @Test
    @DisplayName("Save a single Contract Definition that doesn't already exist")
    void saveOne_doesntExist() {
        var definition = createContractDefinition("id", "policy", "contract");
        getContractDefinitionStore().save(definition);

        var definitions = getContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
    }

    @Test
    @DisplayName("Save a single Contract Definition that already exists")
    void saveOne_alreadyExist_shouldUpdate() {
        getContractDefinitionStore().save(createContractDefinition("id", "policy", "contract"));
        getContractDefinitionStore().save(createContractDefinition("id", "updatedAccess", "updatedContract"));

        var result = getContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(result).hasSize(1).containsExactly(createContractDefinition("id", "updatedAccess", "updatedContract"));
    }

    @Test
    @DisplayName("Save a single Contract Definition that is identical to an existing contract definition except for the id")
    void saveOne_sameParametersDifferentId() {
        var definition1 = createContractDefinition("id1", "policy", "contract");
        var definition2 = createContractDefinition("id2", "policy", "contract");
        getContractDefinitionStore().save(definition1);
        getContractDefinitionStore().save(definition2);

        var definitions = getContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with no preexisting Definitions")
    void saveMany_noneExist() {
        var definitionsCreated = createContractDefinitions(10);
        getContractDefinitionStore().save(definitionsCreated);

        var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).hasSize(definitionsCreated.size());
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with some preexisting Definitions")
    void saveMany_someExist() {
        getContractDefinitionStore().save(createContractDefinitions(3));
        getContractDefinitionStore().save(createContractDefinitions(10));

        var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).hasSize(10);
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with all preexisting Definitions")
    void saveMany_allExist() {
        var definitionsCreated = createContractDefinitions(10);
        getContractDefinitionStore().save(definitionsCreated);

        //
        getContractDefinitionStore().save(definitionsCreated);

        var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved).hasSize(definitionsCreated.size());
    }

    @Test
    @DisplayName("Update a non-existing Contract Definition")
    void updateOne_doesNotExist_shouldCreate() {
        var definition = createContractDefinition("id", "policy1", "contract1");

        getContractDefinitionStore().update(definition);
        var existing = getContractDefinitionStore().findAll(QuerySpec.max());
        assertThat(existing).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(definition);
    }

    @Test
    @DisplayName("Update an existing Contract Definition")
    void updateOne_exists() {
        var definition1 = createContractDefinition("id", "policy1", "contract1");
        var definition2 = createContractDefinition("id", "policy2", "contract2");

        getContractDefinitionStore().save(definition1);
        getContractDefinitionStore().update(definition2);

        var definitions = getContractDefinitionStore().findAll(QuerySpec.none()).collect(Collectors.toList());

        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isEqualTo(1);
        assertThat(definitions.get(0).getContractPolicyId()).isEqualTo(definition2.getContractPolicyId());
        assertThat(definitions.get(0).getAccessPolicyId()).isEqualTo(definition2.getAccessPolicyId());
    }

    @Test
    @DisplayName("Find all contract definitions")
    void findAll() {
        var definitionsExpected = createContractDefinitions(10);
        getContractDefinitionStore().save(definitionsExpected);

        var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved).hasSize(definitionsExpected.size());
    }

    @ParameterizedTest
    @ValueSource(ints = { 49, 50, 51, 100 })
    void findAll_verifyQueryDefaults(int size) {
        var all = IntStream.range(0, size).mapToObj(i -> createContractDefinition("id" + i, "policyId" + i, "contractId" + i))
                .peek(cd -> getContractDefinitionStore().save(cd))
                .collect(Collectors.toList());

        assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).hasSize(size)
                .usingRecursiveFieldByFieldElementComparator()
                .isSubsetOf(all);
    }

    @Test
    @DisplayName("Find all contract definitions with limit and offset")
    void findAll_withSpec() {
        var limit = 20;

        var definitionsExpected = createContractDefinitions(50);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(limit);
    }

    @Test
    @DisplayName("Find all contract definitions that exactly match a particular access policy ID")
    void findAll_queryByAccessPolicyId_withEquals() {

        var definitionsExpected = createContractDefinitions(20);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter("accessPolicyId = policy4")
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .allSatisfy(cd -> assertThat(cd.getId()).isEqualTo("id4"));
    }

    @Test
    @DisplayName("Find all contract definitions that match a range of access policy IDs")
    void findAll_queryByAccessPolicyId_withIn() {

        var definitionsExpected = createContractDefinitions(20);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("accessPolicyId", "in", List.of("policy4", "policy5", "policy6"))))
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(3)
                .usingRecursiveFieldByFieldElementComparator()
                .allMatch(cd -> cd.getId().matches("(id)[4-6]"));
    }


    @Test
    @DisplayName("Verify empty result when query contains a nonexistent value")
    void findAll_queryByNonexistentdValue() {

        var definitionsExpected = createContractDefinitions(20);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractPolicyId", "=", "somevalue")))
                .build();

        assertThat(getContractDefinitionStore().findAll(spec)).isEmpty();
    }

    @Test
    void findAll_invalidOperator() {

        var definitionsExpected = createContractDefinitions(20);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("accessPolicyId", "sqrt", "foobar"))) //sqrt is invalid
                .build();

        assertThatThrownBy(() -> getContractDefinitionStore().findAll(spec)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findById() {
        var id = "definitionId";
        var definition = createContractDefinition(id, "policyId", "contractId");
        getContractDefinitionStore().save(definition);

        var result = getContractDefinitionStore().findById(id);

        assertThat(result).isNotNull().isEqualTo(definition);
    }

    @Test
    void findById_invalidId() {
        assertThat(getContractDefinitionStore().findById("invalid-id")).isNull();
    }

    @Test
    void delete() {
        var definitionExpected = createContractDefinition("test-id1", "policy1", "contract1");
        getContractDefinitionStore().save(definitionExpected);
        assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).hasSize(1);

        var deleted = getContractDefinitionStore().deleteById("test-id1");
        assertThat(deleted).isNotNull().usingRecursiveComparison().isEqualTo(definitionExpected);
        assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).isEmpty();
    }

    @Test
    void delete_whenNotExist() {
        var deleted = getContractDefinitionStore().deleteById("test-id1");
        assertThat(deleted).isNull();
    }

    @Test
    void verifyStore() {
        var definition1 = ContractDefinition.Builder.newInstance().id("1").accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build();
        var definition2 = ContractDefinition.Builder.newInstance().id("2").accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build();

        getContractDefinitionStore().save(definition1);
        assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).contains(definition1);

        getContractDefinitionStore().save(List.of(definition2));
        assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).contains(definition1);

        var deletedDefinition = getContractDefinitionStore().deleteById(definition1.getId());
        assertThat(deletedDefinition).isEqualTo(definition1);
        assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).doesNotContain(definition1);
    }

    @Test
    void deleteById_whenContractDefinitionMissing_returnsNull() {
        assertThat(getContractDefinitionStore().deleteById("not-exists")).isNull();
    }

    @Test
    void findAll_defaultQuerySpec() {
        var all = IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).peek(getContractDefinitionStore()::save).collect(Collectors.toList());
        assertThat(getContractDefinitionStore().findAll(QuerySpec.none())).containsExactlyInAnyOrder(all.toArray(new ContractDefinition[]{}));
    }

    @Test
    void findAll_verifyPaging() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);

        // page size fits
        assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().offset(4).limit(2).build())).hasSize(2);

        // page size larger than collection
        assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void findAll_verifyFiltering() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
        assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().equalsAsContains(false).filter("id=id3").build())).extracting(ContractDefinition::getId)
                .containsOnly("id3");
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
        assertThatThrownBy(() -> getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.sortorder", matches = "true", disabledReason = "This test only runs if sorting is supported")
    void findAll_verifySorting() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);

        assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractDefinition::getId));
        assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryBySelectorExpression_left() {
        var definitionsExpected = createContractDefinitions(20);
        // add a selector expression to the last 5 elements
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        definitionsExpected.get(5).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(format("selectorExpression.criteria.operandLeft = %s", Asset.PROPERTY_ID))
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(2)
                .usingRecursiveFieldByFieldElementComparator()
                .allSatisfy(cd -> assertThat(cd.getId()).matches("id[0,5]"));
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryBySelectorExpression_right() {
        var definitionsExpected = createContractDefinitions(20);
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        definitionsExpected.get(5).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter("selectorExpression.criteria.operandRight = foobar-asset")
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(definitionsExpected.get(5));
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryMultiple() {
        var definitionsExpected = createContractDefinitions(20);
        definitionsExpected.forEach(d -> d.getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset")));
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("selectorExpression.criteria.operandRight", "=", "test-asset"),
                        new Criterion("contractPolicyId", "=", "contract4")))
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(definitionsExpected.get(4));
    }

    @Test
    void find_queryMultiple_noMatch() {
        var definitionsExpected = createContractDefinitions(20);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("selectorExpression.criteria.operandRigh", "=", "test-asset"),
                        new Criterion("contractPolicyId", "=", "contract4")))
                .build();

        assertThat(getContractDefinitionStore().findAll(spec).collect(Collectors.toList())).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryBySelectorExpression_entireCriterion() throws JsonProcessingException {
        var definitionsExpected = createContractDefinitions(20);
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        var def5 = definitionsExpected.get(5);
        def5.getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        getContractDefinitionStore().save(definitionsExpected);

        var json = new ObjectMapper().writeValueAsString(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));

        var spec = QuerySpec.Builder.newInstance()
                .filter("selectorExpression.criteria = " + json)
                .build();

        assertThat(getContractDefinitionStore().findAll(spec)).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(def5);
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.collectionIndexQuery", matches = "true", disabledReason = "This test only runs if collections with index is supported")
    void find_queryBySelectorExpression_withIndex_right() {
        var definitionsExpected = createContractDefinitions(20);
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        definitionsExpected.get(5).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter("selectorExpression.criteria[0].operandRight = foobar-asset")
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(definitionsExpected.get(5));
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.collectionIndexQuery", matches = "true", disabledReason = "This test only runs if collections with index is supported")
    void find_queryBySelectorExpression_withIndex_right_notFound() {
        var definitionsExpected = createContractDefinitions(20);
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        definitionsExpected.get(5).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter("selectorExpression.criteria[1].operandRight = foobar-asset")
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "contractdefinitionstore.supports.sortorder", matches = "true", disabledReason = "This test only runs if sorting is supported")
    void findAll_verifySorting_invalidProperty() {
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        // must actually collect, otherwise the stream is not materialized
        assertThat(getContractDefinitionStore().findAll(query).collect(Collectors.toList())).isEmpty();
    }

    protected abstract ContractDefinitionStore getContractDefinitionStore();

    protected abstract boolean supportsCollectionQuery();

    protected abstract boolean supportsCollectionIndexQuery();

    protected abstract boolean supportsSortOrder();


}
