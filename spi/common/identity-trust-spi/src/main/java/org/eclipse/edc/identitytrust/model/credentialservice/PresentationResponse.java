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

package org.eclipse.edc.identitytrust.model.credentialservice;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A representation of a <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-response-parameters">Presentation Response</a>
 * that the credential service sends back to the requester.
 * <p>
 * The {@code vp_token} param is a JSON String or JSON object that MUST contain a single Verifiable Presentation or an
 * array of JSON Strings and JSON objects, each of them containing a Verifiable Presentations. Each Verifiable Presentation
 * MUST be represented as a JSON string (that is a Base64url encoded value) or a JSON object, depending on the requested format.
 */
public record PresentationResponse(@JsonProperty("vp_token") Object[] vpToken,
                                   @JsonProperty("presentation_submission") PresentationSubmission presentationSubmission) {
}
