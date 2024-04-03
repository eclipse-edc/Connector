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

package org.eclipse.edc.iam.identitytrust.spi.model.credentialservice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Representation of a <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#presentation-submission">DIF Presentation Submission</a>.
 */
public record PresentationSubmission(@JsonProperty("id") String id,
                                     @JsonProperty("definition_id") String definitionId,
                                     @JsonProperty("descriptor_map") List<InputDescriptorMapping> descriptorMap) {
}
