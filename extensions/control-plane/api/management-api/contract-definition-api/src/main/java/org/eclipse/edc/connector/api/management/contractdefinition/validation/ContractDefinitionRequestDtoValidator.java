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

import jakarta.json.JsonString;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.lang.String.format;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERATOR;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class ContractDefinitionRequestDtoValidator {

    public static JsonObjectValidator instance() {
        return JsonObjectValidator.newValidator()
                .verifyId(OptionalIdNotBlank::new)
                .verifyId(IdCannotContainColon::new)
                .verify(CONTRACT_DEFINITION_ACCESSPOLICY_ID, MandatoryValue::new)
                .verify(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, MandatoryValue::new)
                .verifyArrayItem(CONTRACT_DEFINITION_ASSETS_SELECTOR, v -> v
                        .verify(CRITERION_OPERAND_LEFT, MandatoryValue::new)
                        .verify(CRITERION_OPERATOR, MandatoryValue::new)
                )
                .build();
    }

    private record IdCannotContainColon(JsonLdPath path) implements Validator<JsonString> {

        @Override
        public ValidationResult validate(JsonString id) {
            if (id != null && id.getString().contains(":")) {
                var violation = violation(format("'%s' cannot contain ':' character", path), path.toString(), id.getString());
                return ValidationResult.failure(violation);
            }
            return ValidationResult.success();
        }
    }
}
