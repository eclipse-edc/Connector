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

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a Response to a CEL (Common Expression Language) test request.
 *
 */
public class CelExpressionTestResponse {

    public static final String CEL_EXPRESSION_TEST_RESPONSE_TYPE_TERM = "CelExpressionTestResponse";
    public static final String CEL_EXPRESSION_TEST_RESPONSE_TYPE_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_RESPONSE_TYPE_TERM;
    public static final String CEL_EXPRESSION_TEST_RESPONSE_ERROR_TERM = "error";
    public static final String CEL_EXPRESSION_TEST_RESPONSE_ERROR_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_RESPONSE_ERROR_TERM;
    public static final String CEL_EXPRESSION_TEST_RESPONSE_EVALUATION_RESULT_TERM = "evaluationResult";
    public static final String CEL_EXPRESSION_TEST_RESPONSE_EVALUATION_RESULT_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TEST_RESPONSE_EVALUATION_RESULT_TERM;


    private Boolean evaluationResult;
    private String error;


    private CelExpressionTestResponse() {
    }


    public Boolean getEvaluationResult() {
        return evaluationResult;
    }

    public String getError() {
        return error;
    }

    public static class Builder {

        private final CelExpressionTestResponse entity;

        private Builder() {
            entity = new CelExpressionTestResponse();
        }

        public static Builder newInstance() {
            return new Builder();
        }


        public Builder evaluationResult(Boolean evaluationResult) {
            entity.evaluationResult = evaluationResult;
            return this;
        }

        public Builder error(String error) {
            entity.error = error;
            return this;
        }

        public CelExpressionTestResponse build() {
            return entity;

        }

    }
}
