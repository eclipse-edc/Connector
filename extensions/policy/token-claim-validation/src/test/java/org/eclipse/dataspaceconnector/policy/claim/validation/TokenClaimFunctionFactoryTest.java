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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.claim.validation;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.policy.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.dataspaceconnector.policy.claim.validation.TokenClaimFunctionFactory.CLAIM;
import static org.eclipse.dataspaceconnector.policy.claim.validation.TokenClaimFunctionFactory.POLICY_VALIDATION_CONFIG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(EdcExtension.class)
public class TokenClaimFunctionFactoryTest {

    private static final String FOO_CONSTRAINT_NAME = "foo";
    private static final String FOO_CLAIM_NAME = "foo_value";
    private static final String BAR_CONSTRAINT_NAME = "bar";
    private static final String BAR_CLAIM_NAME = "bar_value";

    private final Map<String, String> systemProperties = new HashMap<>() {
        {
            put(POLICY_VALIDATION_CONFIG + "." + FOO_CONSTRAINT_NAME + "." + CLAIM, FOO_CLAIM_NAME);
            put(POLICY_VALIDATION_CONFIG + "." + BAR_CONSTRAINT_NAME + "." + CLAIM, BAR_CLAIM_NAME);
        }
    };

    private AtomicConstraintFunction<Permission> fooClaimValidation;
    private AtomicReference<ServiceExtensionContext> contextRef;

    // mocks
    private PolicyEngine policyEngine;
    private Permission permission;
    private PolicyContext policyContext;
    private ParticipantAgent participantAgent;

    @BeforeEach
    void setup(EdcExtension extension) {
        systemProperties.forEach(System::setProperty);

        this.contextRef = new AtomicReference<>();

        this.policyEngine = Mockito.mock(PolicyEngine.class);
        this.permission = Mockito.mock(Permission.class);
        this.policyContext = Mockito.mock(PolicyContext.class);
        this.participantAgent = Mockito.mock(ParticipantAgent.class);

        extension.registerSystemExtension(ServiceExtension.class, new MyServiceExtension(policyEngine, contextRef));

        Mockito.when(policyContext.getParticipantAgent()).thenReturn(participantAgent);
        Mockito.doAnswer((c) -> {
                            if (c.getArgument(2).equals(FOO_CONSTRAINT_NAME)) {
                                fooClaimValidation = c.getArgument(3);
                            }
                            return null;
                        }
                )
                .when(policyEngine).registerFunction(any(), eq(Permission.class), any(String.class), any());
    }

    @AfterEach
    void tearDown() {
        systemProperties.keySet().forEach(System::clearProperty);
        contextRef.set(null);
    }

    @Test
    public void testRegisterAllRules() {
        Mockito.verify(policyEngine, times(systemProperties.size())).registerFunction(any(), any(), any(), any());
    }

    @Test
    public void testEqualTrue() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(FOO_CLAIM_NAME, "yes"));

        boolean isFooYes = fooClaimValidation.evaluate(Operator.EQ, "yes", permission, policyContext);

        Assertions.assertTrue(isFooYes);
    }

    @Test
    public void testEqualFalse() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(FOO_CLAIM_NAME, "yes"));

        boolean isFooYes = fooClaimValidation.evaluate(Operator.EQ, "no", permission, policyContext);

        Assertions.assertFalse(isFooYes);
    }

    @Test
    public void testEqualNoExists() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(BAR_CLAIM_NAME, "yes"));

        boolean isFooYes = fooClaimValidation.evaluate(Operator.EQ, "no", permission, policyContext);

        Assertions.assertFalse(isFooYes);
    }

    @Test
    public void testContainsTrue() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(FOO_CLAIM_NAME, "yes:yes"));

        boolean isFooContainsYes = fooClaimValidation.evaluate(Operator.IN, "yes", permission, policyContext);

        Assertions.assertTrue(isFooContainsYes);
    }

    @Test
    public void testContainsFalse() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(FOO_CLAIM_NAME, "no:no", BAR_CLAIM_NAME, "yes:yes"));

        boolean isFooContainsYes = fooClaimValidation.evaluate(Operator.IN, "yes", permission, policyContext);

        Assertions.assertFalse(isFooContainsYes);
    }

    @Test
    public void testContainsNoExists() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(BAR_CLAIM_NAME, "no:no"));

        boolean isFooContainsYes = fooClaimValidation.evaluate(Operator.IN, "yes", permission, policyContext);

        Assertions.assertFalse(isFooContainsYes);
    }

    @Test
    public void testNotEqualTrue() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(FOO_CLAIM_NAME, "no"));

        boolean isFooNotYes = fooClaimValidation.evaluate(Operator.NEQ, "yes", permission, policyContext);

        Assertions.assertTrue(isFooNotYes);
    }

    @Test
    public void testNotEqualFalse() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(FOO_CLAIM_NAME, "yes"));

        boolean isFooNotYes = fooClaimValidation.evaluate(Operator.NEQ, "yes", permission, policyContext);

        Assertions.assertFalse(isFooNotYes);
    }

    @Test
    public void testNotEqualNoExists() {
        Mockito.when(participantAgent.getClaims()).thenReturn(Map.of(BAR_CLAIM_NAME, "no"));

        boolean isFooNotYes = fooClaimValidation.evaluate(Operator.NEQ, "yes", permission, policyContext);

        Assertions.assertFalse(isFooNotYes);
    }

    @Provides(PolicyEngine.class)
    private static class MyServiceExtension implements ServiceExtension {
        private final PolicyEngine policyEngine;
        private final AtomicReference<ServiceExtensionContext> contextRef;

        private MyServiceExtension(PolicyEngine policyEngine, AtomicReference<ServiceExtensionContext> contextRef) {
            this.policyEngine = policyEngine;
            this.contextRef = contextRef;
        }

        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(PolicyEngine.class, policyEngine);
            contextRef.set(context);
        }
    }
}
