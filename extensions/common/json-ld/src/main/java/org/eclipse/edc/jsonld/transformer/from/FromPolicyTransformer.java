/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.jsonld.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Transforms a {@link Policy} to an ODRL type as a {@link JsonObject} in expanded JSON-LD form.
 */
public class FromPolicyTransformer extends AbstractJsonLdTransformer<Policy, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public FromPolicyTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(Policy.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable Policy policy, @NotNull TransformerContext context) {
        if (policy == null) {
            return null;
        }
        return policy.accept(new Visitor(context));
    }

    /**
     * Walks the policy object model, transforming it to a JsonObject.
     */
    private static class Visitor implements Policy.Visitor<JsonObject>, Rule.Visitor<JsonObject>, Constraint.Visitor<JsonObject>, Expression.Visitor<JsonObject> {
        private TransformerContext context;

        Visitor(TransformerContext context) {
            this.context = context;
        }

        @Override
        public JsonObject visitAndConstraint(AndConstraint andConstraint) {
            for (var constraint : andConstraint.getConstraints()) {
                var constraintObject = constraint.accept(this);
            }
            return null;
        }

        @Override
        public JsonObject visitOrConstraint(OrConstraint orConstraint) {
            for (var constraint : orConstraint.getConstraints()) {
                var constraintObject = constraint.accept(this);
            }
            return null;
        }

        @Override
        public JsonObject visitXoneConstraint(XoneConstraint xoneConstraint) {
            for (var constraint : xoneConstraint.getConstraints()) {
                var constraintObject = constraint.accept(this);
            }
            return null;
        }

        @Override
        public JsonObject visitAtomicConstraint(AtomicConstraint atomicConstraint) {
            var leftObject = atomicConstraint.getLeftExpression().accept(this);
            var rightObject = atomicConstraint.getRightExpression().accept(this);
            return null;
        }

        @Override
        public JsonObject visitLiteralExpression(LiteralExpression expression) {
            return null;
        }

        @Override
        public JsonObject visitPolicy(Policy policy) {
            policy.getPermissions().forEach(permission -> permission.accept(this));
            policy.getProhibitions().forEach(prohibition -> prohibition.accept(this));
            policy.getObligations().forEach(duty -> duty.accept(this));
            return null;
        }

        @Override
        public JsonObject visitPermission(Permission permission) {
            if (permission.getDuties() != null) {
                for (var duty : permission.getDuties()) {
                    var constraintsArray = visitRule(duty);
                }
            }
            var constraintsArray = visitRule(permission);
            return null;
        }

        @Override
        public JsonObject visitProhibition(Prohibition prohibition) {
            var constraintsArray = visitRule(prohibition);
            return null;
        }

        @Override
        public JsonObject visitDuty(Duty duty) {
            var constraintsArray = visitRule(duty);
            return null;
        }

        private JsonArray visitRule(Rule rule) {
            for (Constraint constraint : rule.getConstraints()) {
                var result = constraint.accept(this);
            }
            return null;
        }

    }
}
