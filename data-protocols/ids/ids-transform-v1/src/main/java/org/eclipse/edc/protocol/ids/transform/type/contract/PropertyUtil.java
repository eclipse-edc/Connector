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

import de.fraunhofer.iais.eis.Contract;
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractOffer;
import org.eclipse.edc.policy.model.Policy;

import java.util.Map;

/**
 * Provides methods to map edc object properties to ids object properties.
 */
public final class PropertyUtil {
    static final String ASSIGNEE_KEY = "edc:policy:assignee";
    static final String ASSIGNER_KEY = "edc:policy:assigner";
    static final String TARGET_KEY = "edc:policy:target";

    /**
     * Add properties from edc policy to an ids contract offer.
     *
     * @param policy edc policy.
     * @param contract ids contract offer.
     * @return the enriched ids contract offer.
     */
    public static ContractOffer addPolicyPropertiesToIdsContract(Policy policy, ContractOffer contract) {
        return (ContractOffer) addProperties(policy, contract);
    }

    /**
     * Add properties from edc policy to an ids contract agreement.
     *
     * @param policy edc policy.
     * @param contract ids contract agreement.
     * @return the enriched ids contract agreement.
     */
    public static ContractAgreement addPolicyPropertiesToIdsContract(Policy policy, ContractAgreement contract) {
        return (ContractAgreement) addProperties(policy, contract);
    }

    /**
     * Add properties from an ids object to the edc policy builder.
     *
     * @param properties custom property map.
     * @param policyBuilder edc policy builder.
     * @return the enriched edc policy builder.
     */
    public static Policy.Builder addIdsContractPropertiesToPolicy(Map<String, Object> properties, Policy.Builder policyBuilder) {
        if (properties != null && !properties.isEmpty()) {
            // add expected edc properties
            var assignee = (String) properties.get(ASSIGNEE_KEY);
            if (assignee != null) {
                policyBuilder.assignee(assignee);
                properties.remove(ASSIGNEE_KEY);
            }

            var assigner = (String) properties.get(ASSIGNER_KEY);
            if (assigner != null) {
                policyBuilder.assigner(assigner);
                properties.remove(ASSIGNER_KEY);
            }

            var target = (String) properties.get(TARGET_KEY);
            if (target != null) {
                policyBuilder.target(target);
                properties.remove(TARGET_KEY);
            }

            // add remaining unexpected properties
            if (!properties.isEmpty()) {
                policyBuilder.extensibleProperties(properties);
            }
        }

        return policyBuilder;
    }

    private static Contract addProperties(Policy policy, Contract contract) {
        var assignee = policy.getAssignee();
        if (assignee != null) {
            contract.setProperty(ASSIGNEE_KEY, assignee);
        }

        var assigner = policy.getAssigner();
        if (assigner != null) {
            contract.setProperty(ASSIGNER_KEY, assigner);
        }

        var target = policy.getTarget();
        if (target != null) {
            contract.setProperty(TARGET_KEY, target);
        }

        if (policy.getExtensibleProperties() != null && !policy.getExtensibleProperties().isEmpty()) {
            for (var key : policy.getExtensibleProperties().keySet()) {
                contract.setProperty(key, policy.getExtensibleProperties().get(key));
            }
        }

        return contract;
    }
}
