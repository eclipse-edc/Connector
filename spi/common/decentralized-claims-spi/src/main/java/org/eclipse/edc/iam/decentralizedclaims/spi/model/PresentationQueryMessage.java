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

package org.eclipse.edc.iam.decentralizedclaims.spi.model;


import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DCP_PREFIX;


/**
 * Represents a query DTO that is sent to a CredentialService. Must be serialized to JSON-LD.
 *
 * @see <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#411-query-for-presentations">DCP Specification</a>
 */
public class PresentationQueryMessage {
    @Deprecated(since = "0.12.0", forRemoval = true)
    public static final String PRESENTATION_QUERY_MESSAGE_SCOPE_PROPERTY = DCP_PREFIX + "scope";
    public static final String PRESENTATION_QUERY_MESSAGE_SCOPE_TERM = "scope";
    @Deprecated(since = "0.12.0", forRemoval = true)
    public static final String PRESENTATION_QUERY_MESSAGE_DEFINITION_PROPERTY = DCP_PREFIX + "presentationDefinition";
    public static final String PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM = "presentationDefinition";
    public static final String PRESENTATION_QUERY_MESSAGE_TERM = "PresentationQueryMessage";
    @Deprecated(since = "0.12.0", forRemoval = true)
    public static final String PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY = DCP_PREFIX + PRESENTATION_QUERY_MESSAGE_TERM;

    private final List<String> scopes = new ArrayList<>();
    private PresentationDefinition presentationDefinition;

    private PresentationQueryMessage() {
    }

    public List<String> getScopes() {
        return scopes;
    }

    public PresentationDefinition getPresentationDefinition() {
        return presentationDefinition;
    }

    public static final class Builder {
        private final PresentationQueryMessage query;

        private Builder() {
            query = new PresentationQueryMessage();
        }

        public static Builder newinstance() {
            return new Builder();
        }

        public Builder scopes(List<String> scopes) {
            this.query.scopes.addAll(scopes);
            return this;
        }

        public Builder scope(String scope) {
            this.query.scopes.add(scope);
            return this;
        }

        public Builder presentationDefinition(PresentationDefinition presentationDefinition) {
            this.query.presentationDefinition = presentationDefinition;
            return this;
        }

        public PresentationQueryMessage build() {
            return query;
        }
    }
}
