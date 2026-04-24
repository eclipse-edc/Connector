/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a CEL (Common Expression Language) test request.
 *
 */
public class CelExpressionTestRequest {

    public static final String CEL_EXPRESSION_TEST_REQUEST_TYPE_TERM = "CelExpressionTestRequest";
    public static final String CEL_EXPRESSION_TEST_REQUEST_TYPE_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_REQUEST_TYPE_TERM;
    public static final String CEL_EXPRESSION_TEST_REQUEST_PARAMS_TERM = "params";
    public static final String CEL_EXPRESSION_TEST_REQUEST_PARAMS_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_REQUEST_PARAMS_TERM;
    public static final String CEL_EXPRESSION_TEST_REQUEST_LEFT_OPERAND_TERM = "leftOperand";
    public static final String CEL_EXPRESSION_TEST_REQUEST_LEFT_OPERAND_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_REQUEST_LEFT_OPERAND_TERM;
    public static final String CEL_EXPRESSION_TEST_REQUEST_OPERATOR_TERM = "operator";
    public static final String CEL_EXPRESSION_TEST_REQUEST_OPERATOR_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_REQUEST_OPERATOR_TERM;
    public static final String CEL_EXPRESSION_TEST_REQUEST_RIGHT_OPERAND_TERM = "rightOperand";
    public static final String CEL_EXPRESSION_TEST_REQUEST_RIGHT_OPERAND_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_REQUEST_RIGHT_OPERAND_TERM;
    public static final String CEL_EXPRESSION_TEST_REQUEST_EXPRESSION_TERM = "expression";
    public static final String CEL_EXPRESSION_TEST_REQUEST_EXPRESSION_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_REQUEST_EXPRESSION_TERM;

    private Map<String, Object> params = new HashMap<>();
    private String leftOperand;
    private Object rightOperand;
    private String operator;
    private String expression;


    private CelExpressionTestRequest() {
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getExpression() {
        return expression;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public String getOperator() {
        return operator;
    }

    public Object getRightOperand() {
        return rightOperand;
    }

    public static class Builder {

        private final CelExpressionTestRequest entity;

        private Builder() {
            entity = new CelExpressionTestRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder params(Map<String, Object> params) {
            entity.params = params;
            return this;
        }

        public Builder param(String key, Object value) {
            entity.params.put(key, value);
            return this;
        }

        public Builder operator(String operator) {
            entity.operator = operator;
            return this;
        }

        public Builder rightOperand(Object rightOperand) {
            entity.rightOperand = rightOperand;
            return this;
        }

        public Builder leftOperand(String leftOperand) {
            entity.leftOperand = leftOperand;
            return this;
        }

        public Builder expression(String expression) {
            entity.expression = expression;
            return this;
        }

        public CelExpressionTestRequest build() {
            Objects.requireNonNull(entity.leftOperand, "CelExpressionTestRequest leftOperand cannot be null");
            Objects.requireNonNull(entity.expression, "CelExpressionTestRequest expression cannot be null");
            Objects.requireNonNull(entity.rightOperand, "CelExpressionTestRequest leftOperand cannot be null");
            Objects.requireNonNull(entity.operator, "CelExpressionTestRequest operator cannot be null");
            return entity;

        }

    }
}
