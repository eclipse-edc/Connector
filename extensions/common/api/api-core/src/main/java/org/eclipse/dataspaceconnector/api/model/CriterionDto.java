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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotNull;

@JsonDeserialize(builder = CriterionDto.Builder.class)
public class CriterionDto {
    @NotNull(message = "operandLeft cannot be null")
    private Object operandLeft;
    @NotNull(message = "operator cannot be null")
    private String operator;
    private Object operandRight;

    private CriterionDto() {

    }

    /**
     * Utility method to easily create a CriterionDto that wraps the builder
     */
    public static CriterionDto from(Object left, String op, Object right) {
        return CriterionDto.Builder.newInstance().operandLeft(left)
                .operator(op)
                .operandRight(right)
                .build();
    }

    public Object getOperandLeft() {
        return operandLeft;
    }

    public String getOperator() {
        return operator;
    }

    public Object getOperandRight() {
        return operandRight;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final CriterionDto dto;

        private Builder() {
            this.dto = new CriterionDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder operandLeft(Object operandLeft) {
            dto.operandLeft = operandLeft;
            return this;
        }

        public Builder operator(String operator) {
            dto.operator = operator;
            return this;
        }

        public Builder operandRight(Object operandRight) {
            dto.operandRight = operandRight;
            return this;
        }

        public CriterionDto build() {
            return dto;
        }
    }
}
