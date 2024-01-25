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

import static org.eclipse.edc.identitytrust.VcConstants.IATP_PREFIX;

/**
 * A representation of a <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#4113-response">Presentation Response</a>
 * that the credential service sends back to the requester.
 * <p>
 * The {@code presentation} param is a JSON String or JSON object that MUST contain a single Verifiable Presentation or an
 * array of JSON Strings and JSON objects, each of them containing a Verifiable Presentations. Each Verifiable Presentation
 * MUST be represented as a JSON string (that is a Base64url encoded value) or a JSON object, depending on the requested format.
 */
public class PresentationResponseMessage {

    public static final String PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_PROPERTY = IATP_PREFIX + "presentation";
    public static final String PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_SUBMISSION_PROPERTY = IATP_PREFIX + "presentationSubmission";
    public static final String PRESENTATION_RESPONSE_MESSAGE_TYPE_PROPERTY = IATP_PREFIX + "PresentationResponseMessage";

    private Object[] presentation = new Object[0];

    private PresentationSubmission presentationSubmission;

    public PresentationSubmission getPresentationSubmission() {
        return presentationSubmission;
    }

    public Object[] getPresentation() {
        return presentation;
    }

    public static final class Builder {
        private final PresentationResponseMessage responseMessage;

        private Builder() {
            responseMessage = new PresentationResponseMessage();
        }

        public static PresentationResponseMessage.Builder newinstance() {
            return new PresentationResponseMessage.Builder();
        }

        public PresentationResponseMessage.Builder presentation(Object[] presentations) {
            this.responseMessage.presentation = presentations;
            return this;
        }

        public PresentationResponseMessage.Builder presentationSubmission(PresentationSubmission presentationSubmission) {
            this.responseMessage.presentationSubmission = presentationSubmission;
            return this;
        }

        public PresentationResponseMessage build() {
            return responseMessage;
        }
    }
}
