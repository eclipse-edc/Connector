/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.policy.model;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public record PolicyEvaluationPlanRequest(String policyScope) {
    public static final String EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE_TERM = "PolicyEvaluationPlanRequest";
    public static final String EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE = EDC_NAMESPACE + EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE_TERM;
    public static final String EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE = EDC_NAMESPACE + "policyScope";
}
