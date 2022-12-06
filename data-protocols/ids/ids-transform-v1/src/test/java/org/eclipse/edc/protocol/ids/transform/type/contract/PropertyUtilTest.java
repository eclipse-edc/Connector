/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial implementation
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.contract;

import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractOfferBuilder;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyUtilTest {
    static final String ASSIGNEE_KEY = "edc:policy:assignee";
    static final String ASSIGNER_KEY = "edc:policy:assigner";
    static final String TARGET_KEY = "edc:policy:target";
    static final String CUSTOM_KEY = "legalText";

    private static final String ASSIGNEE_VALUE = "assignee";
    private static final String ASSIGNER_VALUE = "assigner";
    private static final String TARGET_VALUE = "target";
    private static final String CUSTOM_VALUE = "ref";

    @Test
    void addPolicyPropertiesToIdsContractOffer_null() {
        var policy = getPolicyWithoutProperties();
        var contract = new ContractOfferBuilder().build();

        var result = PropertyUtil.addPolicyPropertiesToIdsContract(policy, contract);

        assertNotNull(result);
        var properties = result.getProperties();
        assertNull(properties);

    }

    @Test
    void addPolicyPropertiesToIdsContractOffer() {
        var policy = getPolicy();
        var contract = new ContractOfferBuilder().build();

        var result = PropertyUtil.addPolicyPropertiesToIdsContract(policy, contract);

        assertNotNull(result);
        var properties = result.getProperties();
        assertFalse(properties.isEmpty());
        assertEquals(ASSIGNEE_VALUE, properties.get(ASSIGNEE_KEY));
        assertEquals(ASSIGNER_VALUE, properties.get(ASSIGNER_KEY));
        assertEquals(TARGET_VALUE, properties.get(TARGET_KEY));
        assertEquals(CUSTOM_VALUE, properties.get(CUSTOM_KEY));
    }

    @Test
    void addPolicyPropertiesToIdsContractAgreement() {
        var policy = getPolicy();
        var contract = new ContractAgreementBuilder().build();

        var result = PropertyUtil.addPolicyPropertiesToIdsContract(policy, contract);

        assertNotNull(result);
        var properties = result.getProperties();
        assertFalse(properties.isEmpty());
        assertEquals(ASSIGNEE_VALUE, properties.get(ASSIGNEE_KEY));
        assertEquals(ASSIGNER_VALUE, properties.get(ASSIGNER_KEY));
        assertEquals(TARGET_VALUE, properties.get(TARGET_KEY));
        assertEquals(CUSTOM_VALUE, properties.get(CUSTOM_KEY));
    }

    @Test
    void addIdsContractPropertiesToPolicy() {
        var map = new HashMap<String, Object>() {{
                put(ASSIGNEE_KEY, ASSIGNEE_VALUE);
                put(ASSIGNER_KEY, ASSIGNER_VALUE);
                put(TARGET_KEY, TARGET_VALUE);
                put(CUSTOM_KEY, CUSTOM_VALUE);
            }};

        var result = PropertyUtil.addIdsContractPropertiesToPolicy(map, Policy.Builder.newInstance());

        var policy = result.build();

        assertNotNull(result);
        assertEquals(ASSIGNEE_VALUE, policy.getAssignee());
        assertEquals(ASSIGNER_VALUE, policy.getAssigner());
        assertEquals(TARGET_VALUE, policy.getTarget());
        assertEquals(CUSTOM_VALUE, policy.getExtensibleProperties().get(CUSTOM_KEY));
    }

    @Test
    void addIdsContractPropertiesToPolicy_empty() {
        var result = PropertyUtil.addIdsContractPropertiesToPolicy(new HashMap<>(), Policy.Builder.newInstance());

        var policy = result.build();

        assertNotNull(result);
        assertNull(policy.getAssignee());
        assertNull(policy.getAssigner());
        assertNull(policy.getTarget());
        assertTrue(policy.getExtensibleProperties().isEmpty());
    }

    @Test
    void addIdsContractPropertiesToPolicy_null() {
        var result = PropertyUtil.addIdsContractPropertiesToPolicy(null, Policy.Builder.newInstance());

        var policy = result.build();

        assertNotNull(result);
        assertNull(policy.getAssignee());
        assertNull(policy.getAssigner());
        assertNull(policy.getTarget());
        assertTrue(policy.getExtensibleProperties().isEmpty());
    }

    private Policy getPolicy() {
        return Policy.Builder.newInstance()
                .target(TARGET_VALUE)
                .assigner(ASSIGNER_VALUE)
                .assignee(ASSIGNEE_VALUE)
                .extensibleProperty("legalText", CUSTOM_VALUE)
                .build();
    }

    private Policy getPolicyWithoutProperties() {
        return Policy.Builder.newInstance().build();
    }
}
