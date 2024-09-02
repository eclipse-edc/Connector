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

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public record PolicyValidationResult(boolean isValid, List<String> errors) {
    public static final String EDC_POLICY_VALIDATION_RESULT_TYPE = EDC_NAMESPACE + "PolicyValidationResult";
    public static final String EDC_POLICY_VALIDATION_RESULT_IS_VALID = EDC_NAMESPACE + "isValid";
    public static final String EDC_POLICY_VALIDATION_RESULT_ERRORS = EDC_NAMESPACE + "errors";
}
