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

package org.eclipse.edc.iam.identitytrust.spi.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DCP_PREFIX;

/**
 * A representation of a <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#4113-response">Presentation Response</a>
 * that the credential service sends back to the requester.
 * <p>
 * The {@code presentation} param is a JSON String or JSON object that MUST contain a single Verifiable Presentation or an
 * array of JSON Strings and JSON objects, each of them containing a Verifiable Presentations. Each Verifiable Presentation
 * MUST be represented as a JSON string (that is a Base64url encoded value) or a JSON object, depending on the requested format.
 */
public class PresentationResponseMessage {

    @Deprecated(since = "0.12.0", forRemoval = true)
    public static final String PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_PROPERTY = DCP_PREFIX + "presentation";
    public static final String PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_TERM = "presentation";
    @Deprecated(since = "0.12.0", forRemoval = true)
    public static final String PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_SUBMISSION_PROPERTY = DCP_PREFIX + "presentationSubmission";
    public static final String PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_SUBMISSION_TERM = "presentationSubmission";
    @Deprecated(since = "0.12.0", forRemoval = true)
    public static final String PRESENTATION_RESPONSE_MESSAGE_TYPE_PROPERTY = DCP_PREFIX + "PresentationResponseMessage";
    public static final String PRESENTATION_RESPONSE_MESSAGE_TYPE_TERM = "PresentationResponseMessage";

    private List<Object> presentation = new ArrayList<>();

    private PresentationSubmission presentationSubmission;

    public PresentationSubmission getPresentationSubmission() {
        return presentationSubmission;
    }

    public List<Object> getPresentation() {
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

        public PresentationResponseMessage.Builder presentation(List<Object> presentations) {
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
