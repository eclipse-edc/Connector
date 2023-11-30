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


import org.eclipse.edc.identitytrust.model.presentationdefinition.PresentationDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.identitytrust.VcConstants.IATP_PREFIX;


/**
 * Represents a query DTO that is sent to a CredentialService. Must be serialized to JSON-LD.
 *
 * @see <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#411-query-for-presentations">IATP Specification</a>
 */
public class PresentationQuery {
    public static final String PRESENTATION_QUERY_SCOPE_PROPERTY = IATP_PREFIX + "scope";
    public static final String PRESENTATION_QUERY_DEFINITION_PROPERTY = IATP_PREFIX + "presentationDefinition";
    public static final String PRESENTATION_QUERY_TYPE_PROPERTY = IATP_PREFIX + "Query";
    private final List<String> scopes = new ArrayList<>();
    private PresentationDefinition presentationDefinition;

    private PresentationQuery() {
    }

    public List<String> getScopes() {
        return scopes;
    }

    public PresentationDefinition getPresentationDefinition() {
        return presentationDefinition;
    }

    public static final class Builder {
        private final PresentationQuery query;

        private Builder() {
            query = new PresentationQuery();
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

        public PresentationQuery build() {
            return query;
        }
    }
}
