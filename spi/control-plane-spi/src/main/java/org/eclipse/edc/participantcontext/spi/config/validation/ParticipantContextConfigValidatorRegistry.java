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

package org.eclipse.edc.participantcontext.spi.config.validation;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.validator.spi.ValidationResult;

/**
 * Registry of the participant context configuration keys consumed by the runtime, used to validate configurations
 * before they are persisted. Extensions reading a key through
 * {@link org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig} should register a
 * {@link ParticipantConfigEntry} for it.
 * <p>
 * Keys that are not registered are accepted, unless the runtime is configured in strict mode.
 */
@ExtensionPoint
public interface ParticipantContextConfigValidatorRegistry {

    /**
     * Registers the description of a configuration key. A later registration for the same key replaces the former.
     *
     * @param entry the entry.
     */
    void register(ParticipantConfigEntry entry);

    /**
     * Validates a write, either a full replacement or a merge patch, of a participant context configuration.
     * <p>
     * The values in {@code write} are validated, and must be submitted as public or private entries according to the
     * sensitivity of their key. {@code null} values are removals and are not validated. Required keys are checked
     * against {@code effective}, the configuration as it would be after the write.
     *
     * @param write     the submitted configuration, with plain text private entries.
     * @param effective the view of the configuration after the write is applied.
     * @return the validation result, carrying all the violations found.
     */
    ValidationResult validateWrite(ParticipantContextConfiguration write, ParticipantConfigView effective);

    /**
     * Checks a stored configuration: every key required by the registered entries must be present, and the registered
     * public entries must be valid and not stored as private entries, and vice versa. The values of private entries
     * are not validated, as they are stored encrypted.
     *
     * @param configuration the view of the configuration.
     * @return the validation result, carrying all the violations found.
     */
    ValidationResult validate(ParticipantConfigView configuration);
}
