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

package org.eclipse.edc.jsonld.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;
import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_SCHEMA;

/**
 * Transforms a {@link Policy} to an ODRL type as a {@link JsonObject} in expanded JSON-LD form.
 */
public class JsonObjectFromPolicyTransformer extends AbstractJsonLdTransformer<Policy, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromPolicyTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(Policy.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable Policy policy, @NotNull TransformerContext context) {
        if (policy == null) {
            return null;
        }
        return policy.accept(new Visitor(context, jsonFactory));
    }

    /**
     * Walks the policy object model, transforming it to a JsonObject.
     */
    private static class Visitor implements Policy.Visitor<JsonObject>, Rule.Visitor<JsonObject>, Constraint.Visitor<JsonObject>, Expression.Visitor<JsonObject> {
        private TransformerContext context;
        private JsonBuilderFactory jsonFactory;

        Visitor(TransformerContext context, JsonBuilderFactory jsonFactory) {
            this.context = context;
            this.jsonFactory = jsonFactory;
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
            var constraintBuilder = jsonFactory.createObjectBuilder();

            constraintBuilder.add("leftOperand", atomicConstraint.getLeftExpression().accept(this));
            constraintBuilder.add("operator", atomicConstraint.getOperator().name());
            constraintBuilder.add("rightOperand", atomicConstraint.getRightExpression().accept(this));

            return constraintBuilder.build();
        }

        @Override
        public JsonObject visitLiteralExpression(LiteralExpression expression) {
            return jsonFactory.createObjectBuilder()
                    .add("@value", Json.createValue(expression.getValue().toString()))
                    .build();
        }

        @Override
        public JsonObject visitPolicy(Policy policy) {
            var permissionsBuilder = jsonFactory.createArrayBuilder();
            policy.getPermissions().forEach(permission -> permissionsBuilder.add(permission.accept(this)));

            var prohibitionsBuilder = jsonFactory.createArrayBuilder();
            policy.getProhibitions().forEach(prohibition -> prohibitionsBuilder.add(prohibition.accept(this)));

            var obligationsBuilder = jsonFactory.createArrayBuilder();
            policy.getObligations().forEach(duty -> obligationsBuilder.add(duty.accept(this)));

            return jsonFactory.createObjectBuilder()
                    .add("@id", randomUUID().toString())
                    .add("@type", ODRL_SCHEMA + getTypeAsString(policy.getType()))
                    .add(CONTEXT, jsonFactory.createObjectBuilder()
                            .add(VOCAB, ODRL_SCHEMA))
                    .add("permission", permissionsBuilder)
                    .add("prohibition", prohibitionsBuilder)
                    .add("obligation", obligationsBuilder)
                    .build();
        }

        @Override
        public JsonObject visitPermission(Permission permission) {
            var permissionBuilder = visitRule(permission);

            if (permission.getDuties() != null && !permission.getDuties().isEmpty()) {
                var dutiesBuilder = jsonFactory.createArrayBuilder();
                for (var duty : permission.getDuties()) {
                    dutiesBuilder.add(visitDuty(duty));
                }
                permissionBuilder.add("duties", dutiesBuilder.build());
            }

            return permissionBuilder.build();
        }

        @Override
        public JsonObject visitProhibition(Prohibition prohibition) {
            var prohibitionBuilder = visitRule(prohibition);

            return prohibitionBuilder.build();
        }

        @Override
        public JsonObject visitDuty(Duty duty) {
            var obligationBuilder = visitRule(duty);

            //TODO consequence (duty), parentPermission?

            return obligationBuilder.build();
        }

        private JsonObjectBuilder visitRule(Rule rule) {
            var ruleBuilder = jsonFactory.createObjectBuilder();

            ruleBuilder.add("action", visitAction(rule.getAction()));
            if (rule.getConstraints() != null && !rule.getConstraints().isEmpty()) {
                ruleBuilder.add("constraint", visitConstraints(rule));
            }

            return ruleBuilder;
        }

        private JsonArray visitConstraints(Rule rule) {
            var constraintsBuilder = jsonFactory.createArrayBuilder();

            for (Constraint constraint : rule.getConstraints()) {
                constraintsBuilder.add(constraint.accept(this));
            }

            return constraintsBuilder.build();
        }

        private JsonObject visitAction(Action action) {
            var actionBuilder = jsonFactory.createObjectBuilder();
            actionBuilder.add("type", action.getType());
            if (action.getIncludedIn() != null) {
                actionBuilder.add("includedIn", action.getIncludedIn());
            }
            if (action.getConstraint() != null) {
                actionBuilder.add("refinement", action.getConstraint().accept(this));
            }
            return actionBuilder.build();
        }
    }

    // Hint: can be removed if internal type "contract" was changed to "agreement"
    private static String getTypeAsString(PolicyType type) {
        switch (type) {
            default:
            case SET:
                return "Set";
            case OFFER:
                return "Offer";
            case CONTRACT:
                return "Agreement";
        }
    }
}
