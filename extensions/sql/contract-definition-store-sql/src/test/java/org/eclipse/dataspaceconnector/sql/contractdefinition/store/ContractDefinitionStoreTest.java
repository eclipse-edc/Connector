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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.contractdefinition.store.TestFunctions.getContractDefinition;
import static org.eclipse.dataspaceconnector.sql.contractdefinition.store.TestFunctions.getContractDefinitions;

abstract class ContractDefinitionStoreTest {


    @Test
    @DisplayName("Save a single Contract Definition that doesn't already exist")
    void saveOne_doesntExist() {
        var definition = getContractDefinition("id", "policy", "contract");
        getSqlContractDefinitionStore().save(definition);

        var definitions = getSqlContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
    }

    @Test
    @DisplayName("Save a single Contract Definition that already exists")
    void saveOne_alreadyExist_shouldUpdate() {
        getSqlContractDefinitionStore().save(getContractDefinition("id", "policy", "contract"));
        getSqlContractDefinitionStore().save(getContractDefinition("id", "updatedAccess", "updatedContract"));

        var result = getSqlContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(result).hasSize(1).containsExactly(getContractDefinition("id", "updatedAccess", "updatedContract"));
    }

    @Test
    @DisplayName("Save a single Contract Definition that is identical to an existing contract definition except for the id")
    void saveOne_sameParametersDifferentId() {
        var definition1 = getContractDefinition("id1", "policy", "contract");
        var definition2 = getContractDefinition("id2", "policy", "contract");
        getSqlContractDefinitionStore().save(definition1);
        getSqlContractDefinitionStore().save(definition2);

        var definitions = getSqlContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with no preexisting Definitions")
    void saveMany_noneExist() {
        var definitionsCreated = getContractDefinitions(10);
        getSqlContractDefinitionStore().save(definitionsCreated);

        var definitionsRetrieved = getSqlContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).hasSize(definitionsCreated.size());
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with some preexisting Definitions")
    void saveMany_someExist() {
        getSqlContractDefinitionStore().save(getContractDefinitions(3));
        getSqlContractDefinitionStore().save(getContractDefinitions(10));

        var definitionsRetrieved = getSqlContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).hasSize(10);
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with all preexisting Definitions")
    void saveMany_allExist() {
        var definitionsCreated = getContractDefinitions(10);
        getSqlContractDefinitionStore().save(definitionsCreated);

        //
        getSqlContractDefinitionStore().save(definitionsCreated);

        var definitionsRetrieved = getSqlContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved).hasSize(definitionsCreated.size());
    }

    @Test
    @DisplayName("Update a non-existing Contract Definition")
    void updateOne_doesNotExist_shouldCreate() {
        var definition = getContractDefinition("id", "policy1", "contract1");

        getSqlContractDefinitionStore().update(definition);
        var existing = getSqlContractDefinitionStore().findAll(QuerySpec.max());
        assertThat(existing).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(definition);
    }

    @Test
    @DisplayName("Update an existing Contract Definition")
    void updateOne_exists() {
        var definition1 = getContractDefinition("id", "policy1", "contract1");
        var definition2 = getContractDefinition("id", "policy2", "contract2");

        getSqlContractDefinitionStore().save(definition1);
        getSqlContractDefinitionStore().update(definition2);

        var definitions = getSqlContractDefinitionStore().findAll(QuerySpec.none()).collect(Collectors.toList());

        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isEqualTo(1);
        assertThat(definitions.get(0).getContractPolicyId()).isEqualTo(definition2.getContractPolicyId());
        assertThat(definitions.get(0).getAccessPolicyId()).isEqualTo(definition2.getAccessPolicyId());
    }

    @Test
    @DisplayName("Find all contract definitions")
    void findAll() {
        var definitionsExpected = getContractDefinitions(10);
        getSqlContractDefinitionStore().save(definitionsExpected);

        var definitionsRetrieved = getSqlContractDefinitionStore().findAll(QuerySpec.max());

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved).hasSize(definitionsExpected.size());
    }

    @ParameterizedTest
    @ValueSource(ints = { 49, 50, 51, 100 })
    void findAll_verifyQueryDefaults(int size) {
        var all = IntStream.range(0, size).mapToObj(i -> getContractDefinition("id" + i, "policyId" + i, "contractId" + i))
                .peek(cd -> getSqlContractDefinitionStore().save(cd))
                .collect(Collectors.toList());

        assertThat(getSqlContractDefinitionStore().findAll(QuerySpec.max())).hasSize(size)
                .usingRecursiveFieldByFieldElementComparator()
                .isSubsetOf(all);
    }

    @Test
    @DisplayName("Find all contract definitions with limit and offset")
    void findAll_withSpec() {
        var limit = 20;

        var definitionsExpected = getContractDefinitions(50);
        getSqlContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        var definitionsRetrieved = getSqlContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(limit);
    }

    @Test
    @DisplayName("Find all contract definitions that exactly match a particular access policy ID")
    void findAll_queryByAccessPolicyId_withEquals() {

        var definitionsExpected = getContractDefinitions(20);
        getSqlContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter("accessPolicyId = policy4")
                .build();

        var definitionsRetrieved = getSqlContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .allSatisfy(cd -> assertThat(cd.getId()).isEqualTo("id4"));
    }

    @Test
    @DisplayName("Find all contract definitions that match a range of access policy IDs")
    void findAll_queryByAccessPolicyId_withIn() {

        var definitionsExpected = getContractDefinitions(20);
        getSqlContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("accessPolicyId", "in", List.of("policy4", "policy5", "policy6"))))
                .build();

        var definitionsRetrieved = getSqlContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(3)
                .usingRecursiveFieldByFieldElementComparator()
                .allMatch(cd -> cd.getId().matches("(id)[4-6]"));
    }

    @Test
    @DisplayName("Verify empty result when query contains invalid keys")
    void findAll_queryByInvalidKey() {

        var definitionsExpected = getContractDefinitions(20);
        getSqlContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("notexist", "=", "somevalue")))
                .build();

        assertThatThrownBy(() -> getSqlContractDefinitionStore().findAll(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

    }

    @Test
    @DisplayName("Verify empty result when query contains a nonexistent value")
    void findAll_queryByNonexistentdValue() {

        var definitionsExpected = getContractDefinitions(20);
        getSqlContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractPolicyId", "=", "somevalue")))
                .build();

        assertThat(getSqlContractDefinitionStore().findAll(spec)).isEmpty();
    }

    @Test
    void findAll_invalidOperator() {

        var definitionsExpected = getContractDefinitions(20);
        getSqlContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("accessPolicyId", "sqrt", "foobar"))) //sqrt is invalid
                .build();

        assertThatThrownBy(() -> getSqlContractDefinitionStore().findAll(spec)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findById() {
        var id = "definitionId";
        var definition = getContractDefinition(id, "policyId", "contractId");
        getSqlContractDefinitionStore().save(definition);

        var result = getSqlContractDefinitionStore().findById(id);

        assertThat(result).isNotNull().isEqualTo(definition);
    }

    @Test
    void findById_invalidId() {
        assertThat(getSqlContractDefinitionStore().findById("invalid-id")).isNull();
    }

    @Test
    void delete() {
        var definitionExpected = getContractDefinition("test-id1", "policy1", "contract1");
        getSqlContractDefinitionStore().save(definitionExpected);
        assertThat(getSqlContractDefinitionStore().findAll(QuerySpec.max())).hasSize(1);

        var deleted = getSqlContractDefinitionStore().deleteById("test-id1");
        assertThat(deleted).isNotNull().usingRecursiveComparison().isEqualTo(definitionExpected);
        assertThat(getSqlContractDefinitionStore().findAll(QuerySpec.max())).isEmpty();
    }

    @Test
    void delete_whenNotExist() {
        var deleted = getSqlContractDefinitionStore().deleteById("test-id1");
        assertThat(deleted).isNull();
    }

    protected abstract SqlContractDefinitionStore getSqlContractDefinitionStore();


}
