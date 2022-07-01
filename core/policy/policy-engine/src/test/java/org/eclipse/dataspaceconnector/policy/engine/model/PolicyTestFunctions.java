/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - resource definition functions
 *
 */

package org.eclipse.dataspaceconnector.policy.engine.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

/**
 * Functions used for testing.
 */
public class PolicyTestFunctions {

    private PolicyTestFunctions() {
    }

    public static AtomicConstraint createLiteralAtomicConstraint(String value1, String value2) {
        LiteralExpression left = new LiteralExpression(value1);
        LiteralExpression right = new LiteralExpression(value2);
        return AtomicConstraint.Builder.newInstance().leftExpression(left).operator(Operator.EQ).rightExpression(right).build();
    }
    
    public static class TestDefinition extends ResourceDefinition {
        String key = "someValue";
    
        public String getKey() {
            return this.key;
        }
    
        public void setKey(String key) {
            this.key = key;
        }
    
        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ResourceDefinition.Builder<TestDefinition, TestDefinition.Builder> {
            private Builder() {
                super(new TestDefinition());
            }
        
            @JsonCreator
            public static TestDefinition.Builder newInstance() {
                return new TestDefinition.Builder();
            }
        
            public TestDefinition.Builder key(String key) {
                resourceDefinition.key = key;
                return this;
            }
        }
    }
    
    public static class RuleTypeArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.arguments(Permission.class),
                    Arguments.arguments(Prohibition.class),
                    Arguments.arguments(Duty.class)
            );
        }
    }
}
