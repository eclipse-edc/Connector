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

package org.eclipse.edc.participantcontext.spi.config.model;

import org.eclipse.edc.validator.spi.Violation;

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Outcome of the validation of a stored participant context configuration.
 *
 * @param participantContextId the participant context identifier.
 * @param violations           the violations found, empty if the configuration is valid.
 */
public record ParticipantContextConfigValidation(String participantContextId, List<Violation> violations) {

    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_TYPE_TERM = "ParticipantContextConfigValidation";
    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_TYPE_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_CONFIG_VALIDATION_TYPE_TERM;
    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_TERM = "valid";
    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_TERM;
    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_TERM = "violations";
    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_TERM;
    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_KEY_IRI = EDC_NAMESPACE + "key";
    public static final String PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_MESSAGE_IRI = EDC_NAMESPACE + "message";

    public ParticipantContextConfigValidation {
        violations = List.copyOf(violations);
    }

    public boolean isValid() {
        return violations.isEmpty();
    }
}
