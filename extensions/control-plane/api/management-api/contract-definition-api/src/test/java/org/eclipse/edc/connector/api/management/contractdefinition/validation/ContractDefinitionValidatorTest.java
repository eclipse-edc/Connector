/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.contractdefinition.validation;

import jakarta.json.JsonArrayBuilder;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERATOR;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ContractDefinitionValidatorTest {

    private final JsonObjectValidator validator = ContractDefinitionValidator.instance();

    @Test
    void shouldSucceed_whenObjectIsValid() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value("accessPolicyId"))
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value("accessPolicyId"))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryFieldsAreMissing() {
        var contractDefinition = createObjectBuilder().build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(CONTRACT_DEFINITION_ACCESSPOLICY_ID))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(CONTRACT_DEFINITION_CONTRACTPOLICY_ID));
    }

    @Test
    void shouldFail_whenIdIsBlank() {
        var contractDefinition = createObjectBuilder()
                .add(ID, " ")
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ID.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenAccessPolicyIdIsBlank() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value(" "))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> CONTRACT_DEFINITION_ACCESSPOLICY_ID.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenContractPolicyIdIsBlank() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value(" "))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> CONTRACT_DEFINITION_CONTRACTPOLICY_ID.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenAssetSelectorCriterionIsNotValid() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value("id"))
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value("id"))
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, createArrayBuilder().add(createObjectBuilder()
                        .add(CRITERION_OPERAND_LEFT, value(" "))
                        .add(CRITERION_OPERATOR, value(" "))
                ))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> assertThat(violation.path()).endsWith(CRITERION_OPERAND_LEFT))
                .anySatisfy(violation -> assertThat(violation.path()).endsWith(CRITERION_OPERATOR));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }

}
