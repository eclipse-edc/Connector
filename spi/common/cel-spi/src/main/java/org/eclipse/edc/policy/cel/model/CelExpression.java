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

import org.eclipse.edc.spi.entity.Entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a CEL (Common Expression Language) expression used in policy definitions.
 *
 */
public class CelExpression extends Entity {

    public static final String CEL_EXPRESSION_TYPE_TERM = "CelExpression";
    public static final String CEL_EXPRESSION_TYPE_IRI = EDC_NAMESPACE + CEL_EXPRESSION_TYPE_TERM;
    public static final String CEL_EXPRESSION_SCOPES_TERM = "scopes";
    public static final String CEL_EXPRESSION_SCOPES_IRI = EDC_NAMESPACE + CEL_EXPRESSION_SCOPES_TERM;
    public static final String CEL_EXPRESSION_LEFT_OPERAND_TERM = "leftOperand";
    public static final String CEL_EXPRESSION_LEFT_OPERAND_IRI = EDC_NAMESPACE + CEL_EXPRESSION_LEFT_OPERAND_TERM;
    public static final String CEL_EXPRESSION_EXPRESSION_TERM = "expression";
    public static final String CEL_EXPRESSION_EXPRESSION_IRI = EDC_NAMESPACE + CEL_EXPRESSION_EXPRESSION_TERM;
    public static final String CEL_EXPRESSION_DESCRIPTION_TERM = "description";
    public static final String CEL_EXPRESSION_DESCRIPTION_IRI = EDC_NAMESPACE + CEL_EXPRESSION_DESCRIPTION_TERM;
    public static final String MATCH_ALL_SCOPE = "*.";

    private Set<String> scopes = new HashSet<>();
    private String leftOperand;
    private String expression;
    private String description;
    private long updatedAt;


    private CelExpression() {
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public String getDescription() {
        return description;
    }

    public String getExpression() {
        return expression;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public static class Builder extends Entity.Builder<CelExpression, Builder> {

        private Builder() {
            super(new CelExpression());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder scopes(Set<String> scopes) {
            entity.scopes = scopes;
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

        public Builder description(String description) {
            entity.description = description;
            return this;
        }

        public Builder updatedAt(Long updatedAt) {
            entity.updatedAt = updatedAt;
            return this;
        }

        @Override
        public CelExpression build() {
            super.build();
            Objects.requireNonNull(entity.leftOperand, "CelExpression leftOperand cannot be null");
            Objects.requireNonNull(entity.expression, "CelExpression expression cannot be null");
            Objects.requireNonNull(entity.description, "CelExpression description cannot be null");
            if (entity.getUpdatedAt() == 0L) {
                entity.updatedAt = entity.getCreatedAt();
            }

            if (entity.scopes.isEmpty()) {
                entity.scopes.add(MATCH_ALL_SCOPE);
            }
            return entity;

        }

    }
}
